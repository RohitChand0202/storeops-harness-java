#!/usr/bin/env bash
# Focused AC-2 / BUILD-2 regression checks. No JDK or Maven is required.
# Run from any directory: bash .harness/checks/fixtures/ac-gates-selftest.sh

set -uo pipefail

CHECKS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
TMP_ROOT="$(mktemp -d)" || exit 2
cleanup() { [ -n "$TMP_ROOT" ] && [ -d "$TMP_ROOT" ] && rm -rf -- "$TMP_ROOT"; }
trap cleanup EXIT

PASS=0
FAIL=0
ok() { printf '  ok    %s\n' "$1"; PASS=$((PASS+1)); }
bad() { printf '  FAIL  %s\n%s\n' "$1" "$OUTPUT"; FAIL=$((FAIL+1)); }

FIXTURE="$TMP_ROOT/project"
mkdir -p "$FIXTURE/.harness/checks"
cp "$CHECKS/ac-gates.sh" "$FIXTURE/.harness/checks/ac-gates.sh"
cat > "$FIXTURE/contract.md" <<'EOF'
MODULES_TOUCHED: activities, shared
LAYERS: service, tests, events
AC-1.1
EOF
git -C "$FIXTURE" init -q || exit 2
git -C "$FIXTURE" add . || exit 2
git -C "$FIXTURE" -c user.name='Gate Selftest' -c user.email='gate@example.invalid' commit -qm baseline || exit 2

run_gate() {
  if OUTPUT="$(cd "$FIXTURE" && bash .harness/checks/ac-gates.sh --contract contract.md "$@" 2>&1)"; then
    STATUS=0
  else
    STATUS=$?
  fi
}

expect_gate() {
  local label="$1" expected_exit="$2" expected_ac2="$3" expected_build2="$4"
  shift 4
  run_gate "$@"
  if [ "$STATUS" -eq "$expected_exit" ] \
    && printf '%s\n' "$OUTPUT" | grep -q "^AC-2: $expected_ac2$" \
    && printf '%s\n' "$OUTPUT" | grep -q "^BUILD-2: $expected_build2$"; then
    ok "$label"
  else
    bad "$label (exit $STATUS; wanted $expected_exit, AC-2 $expected_ac2, BUILD-2 $expected_build2)"
  fi
}

expect_gate 'clean baseline has a decidable empty diff' 0 PASS PASS
expect_gate 'unknown base fails both diff gates' 1 FAIL FAIL --base no-such-ref

mkdir -p "$FIXTURE/src/main/java/com/cognizant/storeops/activities/service"
printf 'class Example {}\n' > "$FIXTURE/src/main/java/com/cognizant/storeops/activities/service/Example.java"
expect_gate 'declared source module and layer pass' 0 PASS PASS

mkdir -p "$FIXTURE/src/main/java/com/cognizant/storeops/shared/event"
printf 'class ExampleEvent {}\n' > "$FIXTURE/src/main/java/com/cognizant/storeops/shared/event/ExampleEvent.java"
expect_gate 'events layer accepts shared/event producer' 0 PASS PASS

printf 'unrelated\n' > "$FIXTURE/README.md"
expect_gate 'untracked non-source file fails scope' 1 FAIL PASS
rm -- "$FIXTURE/README.md"

mkdir -p "$FIXTURE/.harness/output"
printf 'spec\n' > "$FIXTURE/.harness/output/spec.md"
expect_gate 'named working artefact passes scope' 0 PASS PASS

printf '<project/>\n' > "$FIXTURE/pom.xml"
expect_gate 'build configuration fails both gates' 1 FAIL FAIL
rm -- "$FIXTURE/pom.xml"

PARENT="$TMP_ROOT/parent"
mkdir -p "$PARENT/child/.harness/checks"
cp "$CHECKS/ac-gates.sh" "$PARENT/child/.harness/checks/ac-gates.sh"
cp "$FIXTURE/contract.md" "$PARENT/child/contract.md"
git -C "$PARENT" init -q || exit 2
git -C "$PARENT" add . || exit 2
git -C "$PARENT" -c user.name='Gate Selftest' -c user.email='gate@example.invalid' commit -qm baseline || exit 2
if OUTPUT="$(cd "$PARENT/child" && bash .harness/checks/ac-gates.sh --contract contract.md 2>&1)"; then
  STATUS=0
else
  STATUS=$?
fi
if [ "$STATUS" -eq 1 ] \
  && printf '%s\n' "$OUTPUT" | grep -q 'Git root is not the project root' \
  && printf '%s\n' "$OUTPUT" | grep -q '^AC-2: FAIL$' \
  && printf '%s\n' "$OUTPUT" | grep -q '^BUILD-2: FAIL$'; then
  ok 'parent Git repository fails both diff gates'
else
  bad "parent Git repository (exit $STATUS)"
fi

printf '\npassed %d   failed %d\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
