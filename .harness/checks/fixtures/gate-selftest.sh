#!/usr/bin/env bash
# Self-test for the harness gate definitions.
#
# The gates are the only code in this repository with nothing testing them. Everything they govern
# gets ArchUnit, JaCoCo and a review; they got attention. Three defects in them were found by
# reading the configuration rather than running it, and all three produced a *green* build while a
# gate silently did not fire — which is the exact failure the harness exists to prevent, one level
# up. This script is the test that would have caught them.
#
# It builds a tiny project whose source deliberately sits on each gate boundary, splices in the real
# <profile> from ../pom-harness-gates.xml, and asserts what each gate does. It never asserts "the
# build passes" — a gate is only proven by a case it rejects *and* a case it accepts.
#
# Usage, from the repository root:
#
#     .harness/checks/fixtures/gate-selftest.sh
#
# Requires a JDK 17+ and Maven on PATH, and network access on first run to resolve the gate plugins.
# Run it when any file in .harness/checks/ changes, and when the JaCoCo, Checkstyle, SpotBugs or
# ArchUnit version in the profile moves — several of the assertions below depend on tool behaviour
# that is an implementation detail rather than a documented contract.

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKS="$(cd "$HERE/.." && pwd)"
cd "$HERE" || exit 2

PASS=0; FAIL=0
ok()  { printf '  \033[32mok\033[0m    %s\n' "$1"; PASS=$((PASS+1)); }
bad() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL+1)); }

command -v mvn  >/dev/null || { echo "gate-selftest: mvn not on PATH"; exit 2; }
command -v java >/dev/null || { echo "gate-selftest: java not on PATH"; exit 2; }

MVN_FLAGS="-B"
[ -n "${MAVEN_REPO_LOCAL:-}" ] && MVN_FLAGS="$MVN_FLAGS -Dmaven.repo.local=$MAVEN_REPO_LOCAL"

# ---------------------------------------------------------------- assemble

# Splice the real profile in. A self-test against a copy of the gate config proves the copy works.
python - "$CHECKS/pom-harness-gates.xml" "$HERE/pom-template.xml" "$HERE/pom.xml" <<'PY'
import io, sys
prof = io.open(sys.argv[1], encoding="utf-8").read()
prof = prof[prof.index("<profile>"):]
tpl  = io.open(sys.argv[2], encoding="utf-8").read()
io.open(sys.argv[3], "w", encoding="utf-8", newline="\n").write(tpl.replace("<!-- PROFILE -->", prof))
PY
[ -f pom.xml ] || { echo "gate-selftest: could not generate pom.xml"; exit 2; }

# The ArchUnit rules are JUnit tests, so they have to sit in the test tree to run at all.
ARCH_DIR=src/test/java/com/cognizant/storeops/architecture
mkdir -p "$ARCH_DIR"
cp "$CHECKS/ArchitectureRulesTest.java" "$ARCH_DIR/"

# The profile points Checkstyle and SpotBugs at `.harness/checks/*.xml` relative to the project
# root, and this project's root is .harness/checks/fixtures/. Mirror the configuration where the
# profile expects to find it. Copied on every run, from the one place they live, so the self-test
# can never pass against a stale copy — the same constraint that applies to ArchitectureRulesTest.
mkdir -p "$HERE/.harness/checks"
cp "$CHECKS"/*.xml "$HERE/.harness/checks/"

# Not under target/: the skip-flag assertions run `clean`, which would delete the log mid-test.
LOG="$HERE/.selftest.log"
mvn_run() { mvn $MVN_FLAGS "$@" > "$LOG" 2>&1; return $?; }
cleanup() { rm -rf "$ARCH_DIR" "$HERE/.harness" "$HERE/pom.xml"; }
trap cleanup EXIT

echo
echo "Gate self-test — profile spliced from $(basename "$CHECKS")/pom-harness-gates.xml"

# ---------------------------------------------------------------- ERR-1 scope

echo
echo "ERR-1 — scoped to service and web, and nowhere else"

# Task.java throws IllegalArgumentException from a domain constructor; GlobalExceptionHandler
# catches Exception inside shared.error. Both are exempt by checkstyle-suppressions.xml.
mvn_run checkstyle:check@harness-checkstyle-main -Pharness-gates
if [ $? -eq 0 ]; then
  ok "accepts a raw throw in domain/ and a broad catch in shared/error/ (the documented exemptions)"
else
  bad "rejects the documented exemptions — check that suppressions scope by 'id', not 'checks'"
  grep -oE '[A-Za-z]+\.java:[0-9]+.*ERR-1[^ ]*' "$LOG" | head -3 | sed 's/^/        /'
fi

cat > src/main/java/com/cognizant/storeops/activities/service/SelfTestBadService.java <<'EOF'
package com.cognizant.storeops.activities.service;

/** Temporary ERR-1 violation, written and deleted by gate-selftest.sh. */
public class SelfTestBadService {
    /** Throws a raw exception from the service layer. */
    public void go() {
        throw new IllegalStateException("ERR-1 must reject this");
    }
}
EOF
mvn_run checkstyle:check@harness-checkstyle-main -Pharness-gates
if [ $? -ne 0 ] && grep -q 'ERR-1-rawThrow' "$LOG"; then
  ok "rejects a raw throw in service/ and names ERR-1-rawThrow"
else
  bad "a raw throw in service/ did not fail ERR-1 — the gate is not firing"
fi
rm -f src/main/java/com/cognizant/storeops/activities/service/SelfTestBadService.java

# ---------------------------------------------------------------- skip flags

echo
echo "BUILD-1 — no command line can skip the tests the architectural gates live in"

for FLAG in "" "-DskipTests=true" "-Dmaven.test.skip=true" "-Dsurefire.skip=true"; do
  mvn_run clean verify -Pharness-gates $FLAG
  label="${FLAG:-no flag}"
  if grep -q 'ArchitectureRulesTest' "$LOG" && grep -q 'covered ratio' "$LOG"; then
    ok "$label — ArchUnit ran and JaCoCo was enforced"
  else
    bad "$label — gates did not run (ArchUnit: $(grep -qc 'ArchitectureRulesTest' "$LOG" 2>/dev/null || echo 0), JaCoCo enforced: $(grep -c 'covered ratio' "$LOG"))"
  fi
done

# ---------------------------------------------------------------- ArchUnit

echo
echo "ARCH-1..4 and ERR-1 backstop — every rule evaluates on a partial tree"

mvn_run clean test -Pharness-gates
RULES="$(grep -oE 'Tests run: [0-9]+.*ArchitectureRulesTest' "$LOG" | grep -oE 'Tests run: [0-9]+' | grep -oE '[0-9]+' | head -1)"
if [ "${RULES:-0}" -ge 9 ]; then
  ok "all ${RULES} ArchUnit rules executed"
else
  bad "only ${RULES:-0} ArchUnit rules executed, expected 9 or more"
fi
if grep -q 'failed to check any classes' "$LOG"; then
  bad "a rule failed with 'failed to check any classes' — add .allowEmptyShould(true) to module-scoped rules"
elif grep -qE "Layer '[A-Za-z]+' is empty" "$LOG"; then
  bad "layeredArchitecture reported an empty layer as a violation — use optionalLayer()"
else
  ok "no rule failed merely because its subject package is absent"
fi

# ---------------------------------------------------------------- TEST-1

echo
echo "TEST-1 — per-package coverage thresholds actually bind"

mvn_run clean verify -Pharness-gates
if grep -q 'Rule violated for package com.cognizant.storeops.activities.service' "$LOG"; then
  ok "the service package rule fires by name (patterns match dotted package names)"
  grep -oE 'Rule violated for package [^,]+, but expected minimum is [0-9.]+' "$LOG" | sed 's/^/        /'
else
  bad "no per-package rule fired on an under-covered service package — only the BUNDLE rule is binding"
  grep -oE 'Rule violated[^"]*' "$LOG" | head -3 | sed 's/^/        /'
fi

# ---------------------------------------------------------------- SpotBugs

echo
echo "BUILD-1 — SpotBugs does not reject what ERR-1 permits"

if grep -qE 'CT_CONSTRUCTOR_THROW|will leave the constructor' "$LOG"; then
  bad "SpotBugs rejects a validating domain constructor, which ERR-1 explicitly allows — see spotbugs-excludes.xml"
else
  ok "a validating domain constructor passes SpotBugs"
fi

# ---------------------------------------------------------------- done

echo
printf 'passed %d   failed %d\n\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] || { echo "A gate is not behaving as its configuration claims. Fix it before trusting a verdict."; exit 1; }
echo "Every gate rejected what it should reject and accepted what it should accept."
exit 0
