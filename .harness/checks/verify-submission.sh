#!/usr/bin/env bash
# Pre-submission check for the StoreOps harness capstone.
#
# The 10% "harness ran end-to-end" criterion is scored on the artefact chain being internally
# consistent: spec references the prompt, contract references the spec, generator summary references
# the contract's ACs, evaluator feedback references files from the summary. Reviewers follow that
# chain by hand. This script follows it first.
#
# It checks structure and cross-references. It cannot check whether your reasoning is any good.
#
#   bash .harness/checks/verify-submission.sh

set -uo pipefail

PASS=0; FAIL=0; WARN=0
ok()   { printf '  \033[32mok\033[0m    %s\n' "$1"; PASS=$((PASS+1)); }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL+1)); }
warn() { printf '  \033[33mwarn\033[0m  %s\n' "$1"; WARN=$((WARN+1)); }

have() { [ -f "$1" ]; }
has()  { grep -qi -- "$2" "$1" 2>/dev/null; }

echo
echo "Required files (Section 7)"
for f in CLAUDE.md PROMPT.md DESIGN_BRIEF.md REFLECTION.md DEPLOYMENT.md; do
  have "$f" && ok "$f" || bad "$f missing"
done
for f in planner generator evaluator monitor; do
  have ".harness/agents/$f.agent.md" && ok ".harness/agents/$f.agent.md" \
    || bad ".harness/agents/$f.agent.md missing"
done

echo
echo "Gate definitions (.harness/checks/)"
for f in ArchitectureRulesTest.java checkstyle-harness.xml checkstyle-suppressions.xml \
         checkstyle-build2-tests.xml spotbugs-excludes.xml pom-harness-gates.xml ac-gates.sh \
         fixtures/gate-selftest.sh; do
  have ".harness/checks/$f" && ok ".harness/checks/$f" || bad ".harness/checks/$f missing"
done
for s in ac-gates.sh fixtures/gate-selftest.sh; do
  have ".harness/checks/$s" && { bash -n ".harness/checks/$s" 2>/dev/null \
    && ok "$s parses" || bad "$s has a syntax error"; }
done

# Each of the three below is a defect that shipped once and produced a green build. The gate
# self-test is the real check; these are the cheap regression guards for the specific mistakes.
grep -q 'id="ERR-1-rawThrow"' .harness/checks/checkstyle-suppressions.xml 2>/dev/null \
  && ok "ERR-1 suppressions scope by id, not checks" \
  || bad "checkstyle-suppressions.xml scopes by 'checks' — Checkstyle matches that against the check class, so ERR-1 is unscoped"
grep -q 'default-testCompile' .harness/checks/pom-harness-gates.xml 2>/dev/null \
  && ok "compiler testCompile pinned — -Dmaven.test.skip cannot skip the arch gates" \
  || bad "pom-harness-gates.xml does not pin maven-compiler-plugin testCompile — -Dmaven.test.skip=true gives a green build with zero gates run"
grep -q 'failIfNoTests' .harness/checks/pom-harness-gates.xml 2>/dev/null \
  && ok "surefire failIfNoTests set — 'zero tests' is an error, not a pass" \
  || warn "surefire does not set failIfNoTests — a run with no tests reports success"
grep -q 'allowEmptyShould' .harness/checks/ArchitectureRulesTest.java 2>/dev/null \
  && ok "module-scoped ArchUnit rules allow an empty subject" \
  || bad "ArchitectureRulesTest has no allowEmptyShould — rules FAIL on a tree where their module is absent"

echo
echo "Skill files (minimum 6)"
SKILLS=$(find .harness/skills -name SKILL.md 2>/dev/null | wc -l | tr -d ' ')
[ "$SKILLS" -ge 6 ] && ok "$SKILLS skill files" || bad "$SKILLS skill files, minimum is 6"
for s in app-context architecture-principles; do
  have ".harness/skills/$s/SKILL.md" && ok "shared: $s" || bad "shared skill $s missing"
done

echo
echo "Run artefacts in .harness/reviews/ (28%)"
REV=.harness/reviews
for pat in "*contract*" "*generator-summary*" "*evaluator-feedback*" "*run-log*"; do
  n=$(find "$REV" -name "$pat" ! -name "*TEMPLATE*" 2>/dev/null | wc -l | tr -d ' ')
  [ "$n" -ge 1 ] && ok "$pat  ($n)" || bad "no $pat committed — this is the 28%"
done
find "$REV" -name "*TEMPLATE*" 2>/dev/null | grep -q . \
  && bad "a TEMPLATE file is in reviews/ — templates are reference shapes, not run output"

echo
echo "Artefact chain"
SPEC=$(find "$REV" .harness/output -name "spec.md" 2>/dev/null | head -1)
CONTRACT=$(find "$REV" -name "*contract*" ! -name "*TEMPLATE*" 2>/dev/null | head -1)
SUMMARY=$(find "$REV" -name "*generator-summary*" ! -name "*TEMPLATE*" 2>/dev/null | head -1)
FEEDBACK=$(find "$REV" -name "*evaluator-feedback*" ! -name "*TEMPLATE*" 2>/dev/null | head -1)
LOG=$(find "$REV" -name "*run-log*" ! -name "*TEMPLATE*" 2>/dev/null | head -1)

if have "${SPEC:-/nonexistent}"; then
  has "$SPEC" "AWAITING APPROVAL" && ok "spec.md carries the approval marker" \
    || warn "spec.md has no STATUS: AWAITING APPROVAL marker"
else
  bad "spec.md not found in reviews/ or output/"
fi

if have "${CONTRACT:-/nonexistent}"; then
  grep -qE 'GIVEN' "$CONTRACT" && grep -qE 'WHEN' "$CONTRACT" && grep -qE 'THEN' "$CONTRACT" \
    && ok "contract uses GIVEN/WHEN/THEN" || bad "contract has no GIVEN/WHEN/THEN acceptance criteria"
  # -o and -c together count matching LINES, not matches, and every AC id appears on several lines.
  # Count distinct ids instead, which is what "how many ACs" means.
  ACS=$(grep -oE 'AC-[0-9]+\.[0-9]+' "$CONTRACT" | sort -u | wc -l | tr -d ' ')
  [ "$ACS" -ge 3 ] && ok "$ACS acceptance criteria" || warn "only $ACS ACs — thin for a sprint"
  grep -qE '^[[:space:]]*MODULES_TOUCHED[[:space:]]*:' "$CONTRACT" \
    && ok "contract header has MODULES_TOUCHED" \
    || bad "contract has no MODULES_TOUCHED header — gate AC-2 cannot be decided without it"
  grep -qE '^[[:space:]]*LAYERS[[:space:]]*:' "$CONTRACT" \
    && ok "contract header has LAYERS" \
    || bad "contract has no LAYERS header — gate AC-2 cannot be decided without it"
fi

if have "${SUMMARY:-/nonexistent}" && have "${CONTRACT:-/nonexistent}"; then
  MISSING=""
  for ac in $(grep -oE 'AC-[0-9]+\.[0-9]+' "$CONTRACT" | sort -u); do
    grep -q "$ac" "$SUMMARY" || MISSING="$MISSING $ac"
  done
  [ -z "$MISSING" ] && ok "every contract AC appears in the generator summary" \
    || bad "ACs missing from summary:$MISSING — gate AC-1 requires evidence per AC"
  has "$SUMMARY" "## AC self-check"  && ok "summary has AC self-check"  || bad "summary missing '## AC self-check'"
  has "$SUMMARY" "## Files changed"  && ok "summary has Files changed"  || bad "summary missing '## Files changed'"
  has "$SUMMARY" "## Events emitted" && ok "summary has Events emitted" || bad "summary missing '## Events emitted'"
  has "$SUMMARY" "## Known gaps"     && ok "summary has Known gaps"     || bad "summary missing '## Known gaps'"
fi

if have "${FEEDBACK:-/nonexistent}"; then
  grep -qE '^VERDICT: (PASS|CONDITIONAL PASS|FAIL)' "$FEEDBACK" \
    && ok "feedback has a parseable VERDICT line" || bad "feedback has no machine-readable VERDICT line"
  grep -qE '^HARD_GATES_FAILED:' "$FEEDBACK" && ok "feedback declares HARD_GATES_FAILED" \
    || bad "feedback missing HARD_GATES_FAILED"
  grep -qE '\.java:[0-9]+' "$FEEDBACK" && ok "feedback cites file:line references" \
    || warn "no file:line references in feedback — the rubric asks for line-level specificity"
  if grep -qE '^VERDICT: FAIL' "$FEEDBACK"; then
    has "$FEEDBACK" "## Required fixes" && ok "FAIL verdict has Required fixes" \
      || bad "FAIL verdict with no '## Required fixes' — the Generator has nothing to act on"
  fi
fi

if have "${LOG:-/nonexistent}"; then
  for k in ITERATIONS_USED ESCALATED TOKEN_COST_ESTIMATE VERDICT; do
    grep -qE "^$k:" "$LOG" && ok "run-log has $k" || bad "run-log missing $k"
  done
fi

echo
echo "Deployment evidence (4%)"
if have DEPLOYMENT.md; then
  grep -qiE '\[fill in\]|\[paste|TO COMPLETE' DEPLOYMENT.md \
    && bad "DEPLOYMENT.md still has placeholders" || ok "DEPLOYMENT.md has no placeholders left"
  find docs -maxdepth 1 -iname '*.png' -o -maxdepth 1 -iname '*.jpg' 2>/dev/null | grep -q . \
    && ok "screenshots committed in docs/" || warn "no screenshots in docs/ — commit them or give a live URL"
fi

echo
echo "Reflection (6%)"
if have REFLECTION.md; then
  grep -qiE 'TO COMPLETE|\[fill in\]|delete this block' REFLECTION.md \
    && bad "REFLECTION.md is still the scaffold — this is 6% and reviewers spot it instantly" \
    || ok "REFLECTION.md written"
  grep -qE '\bD[123]\b' REFLECTION.md && ok "reflection references a Design Brief decision" \
    || warn "reflection does not name a Design Brief decision (D1/D2/D3) — the rubric asks for that link"
  W=$(wc -w < REFLECTION.md | tr -d ' ')
  [ "$W" -le 700 ] && ok "reflection is ~1 page ($W words)" || warn "reflection is $W words, max is 1 page"
fi

echo
echo "Design brief"
if have DESIGN_BRIEF.md; then
  for sec in "Section A" "Section B" "Section C" "Section D"; do
    has DESIGN_BRIEF.md "$sec" && ok "brief has $sec" || bad "brief missing $sec"
  done
  W=$(wc -w < DESIGN_BRIEF.md | tr -d ' ')
  [ "$W" -le 3000 ] && ok "brief is $W words (~3-5 pages)" || warn "brief is $W words — likely over 5 pages"
fi

echo
echo "Hygiene"
have .gitignore && has .gitignore ".harness/output" && ok ".harness/output/ is gitignored" \
  || warn ".harness/output/ should be gitignored during a run"
if [ -d .git ]; then
  git ls-files --error-unmatch .env >/dev/null 2>&1 && bad ".env is tracked — remove it" || ok ".env not tracked"
  git log --oneline 2>/dev/null | grep -qi "baseline" && ok "baseline commit present" \
    || warn "no commit mentioning 'baseline' — reviewers diff the run against it"
else
  warn "not a git repository yet"
fi

echo
printf 'passed %d   failed %d   warnings %d\n\n' "$PASS" "$FAIL" "$WARN"
[ "$FAIL" -eq 0 ] || { echo "Fix the failures before submitting."; exit 1; }
[ "$WARN" -eq 0 ] || echo "No failures. Read the warnings — most are scored criteria."
exit 0
