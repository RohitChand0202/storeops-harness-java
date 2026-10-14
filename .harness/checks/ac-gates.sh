#!/usr/bin/env bash
# Automated halves of gates AC-1, AC-2 and BUILD-2.
#
# Dimension 2 (Acceptance criteria satisfaction) is the one dimension whose gates were both decided
# by the Evaluator reading something. That made it the weakest dimension in the framework: the two
# gates protecting the link between verdict and contract had no tool behind them, so an LLM assessor
# was the only thing standing between a skipped AC and a PASS. This script is the tool.
#
# It decides, by exit code, the parts of those three gates that are countable:
#
#   AC-1  every AC-<n>.<m> in the contract appears in the generator summary's AC self-check with a
#         `Class#method` citation, and that method exists in the test tree. Fails on a missing AC
#         row, on the literal NOT IMPLEMENTED, and on a citation that resolves to nothing.
#         The Evaluator still reads each cited test to confirm it asserts the AC's observables —
#         that half cannot be automated. This half no longer needs to be.
#
#   AC-2  every changed source file maps to a module in MODULES_TOUCHED and a layer in LAYERS,
#         both parsed out of the contract header. Non-source changes are rejected except the
#         named working artefacts produced by the Planner, Generator and Evaluator.
#
#   BUILD-2  the diff does not touch pom.xml or the harness's own governance files. Lowering a
#         JaCoCo threshold produces a *passing* build by design, so no linter can see it; this is
#         the diff scan that evaluation-criteria used to leave to the Evaluator's eye.
#
# Usage:
#   .harness/checks/ac-gates.sh --contract <path> [--summary <path>] [--base <git-ref>]
#
#   --contract  sprint-N-contract.md                  (required)
#   --summary   generator-summary.md                  (optional; AC-1 is skipped without it)
#   --base      ref to diff against, default HEAD     (use the baseline or last passing sprint)
#
# Run from the project root. The diff is tracked changes against --base plus untracked new files,
# because a file the Generator has just written is part of the sprint's diff whether or not it is
# staged. That means the repository
# needs the `baseline: generated StoreOps scaffold` commit before AC-2 and BUILD-2 mean anything: on a
# repository with no commits every file is untracked, so everything reads as in-scope-or-violating.
# An invalid --base, another current directory, or a parent Git repository makes both diff gates
# UNDETERMINED and fails the command. An empty diff is only accepted after these checks pass.
#
# Exit 0 only when every check it was given the inputs to decide passed. Any failure exits 1.
#
# Output contract. The last block is fixed-format and quotable verbatim in the Evaluator's
# `## Gate results` section:
#
#   AC-1: PASS | FAIL | SKIPPED
#   AC-2: PASS | FAIL
#   BUILD-2: PASS | FAIL
#
# A check this script cannot decide — a contract with no MODULES_TOUCHED header, a summary with no
# AC self-check table — prints `UNDETERMINED:` and exits 1, per the fail-closed rule in
# evaluation-criteria. An undecidable gate is a FAIL whose required fix is to make it decidable.

set -uo pipefail

CONTRACT=""
SUMMARY=""
BASE="HEAD"
TEST_ROOT="src/test/java"

while [ $# -gt 0 ]; do
  case "$1" in
    --contract) CONTRACT="${2:-}"; shift 2 ;;
    --summary)  SUMMARY="${2:-}";  shift 2 ;;
    --base)     BASE="${2:-}";     shift 2 ;;
    --test-root) TEST_ROOT="${2:-}"; shift 2 ;;
    -h|--help)  sed -n '2,45p' "$0"; exit 0 ;;
    *) printf 'ac-gates: unknown argument %s\n' "$1" >&2; exit 2 ;;
  esac
done

fail_lines=()
note()  { printf '  %s\n' "$1"; }
bad()   { printf '  FAIL  %s\n' "$1"; fail_lines+=("$1"); }
undet() { printf '  UNDETERMINED: %s\n' "$1"; fail_lines+=("UNDETERMINED: $1"); }

if [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then
  printf 'ac-gates: --contract is required and must exist\n' >&2
  exit 2
fi

# ---------------------------------------------------------------- contract header

# Header fields are `KEY: value` on one line. Take the first occurrence so a value quoted later in
# the prose cannot change the gate's answer.
header_value() {
  grep -m1 -E "^[[:space:]]*$1[[:space:]]*:" "$CONTRACT" 2>/dev/null \
    | sed -E "s/^[[:space:]]*$1[[:space:]]*:[[:space:]]*//" \
    | tr -d '\r' \
    | tr 'A-Z' 'a-z'
}

# Split a comma/pipe/space separated field into sorted unique tokens.
tokens() {
  printf '%s\n' "$1" \
    | tr ',|' '\n' \
    | sed -E 's/^[[:space:]]*//; s/[[:space:]]*$//; s/^`//; s/`$//' \
    | grep -v '^$' \
    | sort -u
}

MODULES_RAW="$(header_value MODULES_TOUCHED)"
LAYERS_RAW="$(header_value LAYERS)"

AC2_STATUS="PASS"
AC1_STATUS="SKIPPED"
BUILD2_STATUS="PASS"

echo
echo "Contract: $CONTRACT"

if [ -z "$MODULES_RAW" ]; then
  undet "contract has no MODULES_TOUCHED header — AC-2 cannot be decided (planner.agent.md defines the field)"
  AC2_STATUS="FAIL"
fi
if [ -z "$LAYERS_RAW" ]; then
  undet "contract has no LAYERS header — AC-2 cannot be decided"
  AC2_STATUS="FAIL"
fi

MODULES="$(tokens "$MODULES_RAW")"
LAYERS="$(tokens "$LAYERS_RAW")"
note "MODULES_TOUCHED: $(printf '%s' "$MODULES" | tr '\n' ' ')"
note "LAYERS:          $(printf '%s' "$LAYERS" | tr '\n' ' ')"

in_set() { printf '%s\n' "$2" | grep -qx -- "$1"; }

# ---------------------------------------------------------------- diff

# Git resolves paths relative to the caller's directory. Require the caller and Git repository to
# be this project's root, or a parent repository could make a missing project baseline look valid.
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
CHANGED=""
DIFF_READY="yes"
if [ "$(pwd -P)" != "$PROJECT_ROOT" ]; then
  undet "run ac-gates.sh from the project root '$PROJECT_ROOT' — AC-2 and BUILD-2 cannot be decided here"
  DIFF_READY="no"
elif ! GIT_TOP="$(git rev-parse --show-toplevel 2>/dev/null)"; then
  undet "not a git repository — AC-2 and BUILD-2 are diff-based and cannot be decided"
  DIFF_READY="no"
elif ! GIT_ROOT="$(cd "$GIT_TOP" 2>/dev/null && pwd -P)" || [ "$GIT_ROOT" != "$PROJECT_ROOT" ]; then
  undet "Git root is not the project root '$PROJECT_ROOT' — AC-2 and BUILD-2 cannot use a parent repository"
  DIFF_READY="no"
elif [ -z "$BASE" ] || ! git rev-parse --verify -q --end-of-options "${BASE}^{commit}" >/dev/null 2>&1; then
  undet "base '$BASE' does not resolve to a commit — AC-2 and BUILD-2 cannot be decided"
  DIFF_READY="no"
fi

if [ "$DIFF_READY" = "yes" ]; then
  # A diff against BASE includes staged and unstaged tracked changes. Include untracked files too.
  if ! TRACKED="$(git diff --name-only --no-renames "$BASE" -- 2>&1)"; then
    undet "git diff against '$BASE' failed — AC-2 and BUILD-2 cannot be decided: $TRACKED"
    DIFF_READY="no"
  elif ! UNTRACKED="$(git ls-files --others --exclude-standard 2>&1)"; then
    undet "git ls-files failed — AC-2 and BUILD-2 cannot be decided: $UNTRACKED"
    DIFF_READY="no"
  else
    CHANGED="$(printf '%s\n%s\n' "$TRACKED" "$UNTRACKED" | sed 's|\\|/|g' | grep -v '^$' | sort -u)"
  fi
fi

if [ "$DIFF_READY" = "no" ]; then
  AC2_STATUS="FAIL"; BUILD2_STATUS="FAIL"
fi

echo
echo "AC-2 — diff scope against MODULES_TOUCHED x LAYERS"

if [ "$DIFF_READY" = "no" ]; then
  note "diff unavailable"
elif [ -z "$CHANGED" ]; then
  note "no changed files against $BASE"
else
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
      src/main/java/com/cognizant/storeops/*|src/test/java/com/cognizant/storeops/*) : ;;
      src/*) bad "AC-2: $f is under src/ but outside com/cognizant/storeops — not a module path"
             AC2_STATUS="FAIL"; continue ;;
      .harness/output/spec.md|.harness/output/generator-summary.md|.harness/output/evaluator-feedback.md|.harness/output/escalation.md|.harness/output/sprint-[0-9]*-contract.md)
        note "working artefact: $f"; continue ;;
      *) bad "AC-2: $f is outside src/ and is not a declared working artefact"
         AC2_STATUS="FAIL"; continue ;;
    esac

    rest="${f#src/*/java/com/cognizant/storeops/}"
    module="${rest%%/*}"
    tail_path="${rest#*/}"
    layer="${tail_path%%/*}"

    # A file directly in the storeops root (StoreOpsApplication.java) has no module segment.
    if [ "$module" = "$rest" ]; then
      bad "AC-2: $f sits at the package root — no sprint contract owns it"
      AC2_STATUS="FAIL"; continue
    fi

    if ! in_set "$module" "$MODULES"; then
      bad "AC-2: $f — module '$module' is not in MODULES_TOUCHED"
      AC2_STATUS="FAIL"; continue
    fi

    # `listener` and `event` contain event consumers and producers; a contract declaring the
    # `events` layer covers both. `tests` covers anything under src/test.
    ok_layer="no"
    in_set "$layer" "$LAYERS" && ok_layer="yes"
    case "$layer" in
      listener|event) in_set events "$LAYERS" && ok_layer="yes" ;;
    esac
    case "$f" in src/test/*) in_set tests "$LAYERS" && ok_layer="yes" ;; esac

    if [ "$ok_layer" = "no" ]; then
      bad "AC-2: $f — layer '$layer' is not in LAYERS"
      AC2_STATUS="FAIL"; continue
    fi

    note "in scope: $f  ($module/$layer)"
  done <<< "$CHANGED"
fi

[ "$AC2_STATUS" = "PASS" ] && note "AC-2 PASS — every changed src/ file is inside the declared scope"

# ---------------------------------------------------------------- BUILD-2 diff scan

echo
echo "BUILD-2 — governance files and build configuration in the diff"

if [ -n "$CHANGED" ]; then
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
      pom.xml|*/pom.xml)
        bad "BUILD-2: $f is in the diff. Thresholds and check configuration change in the open, not in a feature commit"
        BUILD2_STATUS="FAIL" ;;
      .harness/checks/*|.harness/skills/*|.harness/agents/*)
        bad "BUILD-2: $f is in the diff. The Generator is governed by this file and cannot amend its own governance"
        BUILD2_STATUS="FAIL" ;;
      */ArchitectureRulesTest.java)
        bad "BUILD-2: $f is in the diff. The architecture gates are not editable to make a build pass"
        BUILD2_STATUS="FAIL" ;;
    esac
  done <<< "$CHANGED"
fi

[ "$BUILD2_STATUS" = "PASS" ] \
  && note "BUILD-2 PASS — no build configuration or governance file in the diff (Checkstyle decides the suppression halves)"

# ---------------------------------------------------------------- AC-1 citation integrity

echo
echo "AC-1 — citation integrity (the Evaluator still reads each cited test)"

ACS="$(grep -oE 'AC-[0-9]+\.[0-9]+' "$CONTRACT" 2>/dev/null | sort -u)"

if [ -z "$ACS" ]; then
  undet "contract contains no AC-<n>.<m> identifiers — AC-1 cannot be decided"
  AC1_STATUS="FAIL"
elif [ -z "$SUMMARY" ]; then
  note "no --summary given; AC-1 not decided here"
elif [ ! -f "$SUMMARY" ]; then
  undet "summary '$SUMMARY' does not exist — AC-1 cannot be decided"
  AC1_STATUS="FAIL"
elif ! grep -q '^## AC self-check' "$SUMMARY"; then
  undet "summary has no '## AC self-check' section — AC-1 cannot be decided"
  AC1_STATUS="FAIL"
else
  AC1_STATUS="PASS"
  note "contract declares $(printf '%s\n' "$ACS" | grep -c .) acceptance criteria"

  for ac in $ACS; do
    row="$(grep -m1 -E "^\|[[:space:]]*$ac\b" "$SUMMARY" 2>/dev/null | tr -d '\r')"
    if [ -z "$row" ]; then
      bad "AC-1: $ac has no row in the summary's AC self-check table"
      AC1_STATUS="FAIL"; continue
    fi

    evidence="$(printf '%s' "$row" | awk -F'|' '{print $4}' \
      | sed -E 's/^[[:space:]]*//; s/[[:space:]]*$//; s/`//g')"

    if [ -z "$evidence" ] || [ "$evidence" = "—" ] || [ "$evidence" = "-" ]; then
      bad "AC-1: $ac has an empty Evidence cell — cite a test method or the literal NOT IMPLEMENTED"
      AC1_STATUS="FAIL"; continue
    fi
    if printf '%s' "$evidence" | grep -qi 'NOT IMPLEMENTED'; then
      bad "AC-1: $ac is declared NOT IMPLEMENTED — an unimplemented AC fails the gate by definition"
      AC1_STATUS="FAIL"; continue
    fi
    if ! printf '%s' "$evidence" | grep -qE '^[A-Za-z_][A-Za-z0-9_]*#[A-Za-z_][A-Za-z0-9_]*$'; then
      bad "AC-1: $ac cites '$evidence', which is not a Class#method reference (a source file is not evidence)"
      AC1_STATUS="FAIL"; continue
    fi

    cls="${evidence%%#*}"
    mth="${evidence##*#}"

    if [ ! -d "$TEST_ROOT" ]; then
      undet "test root '$TEST_ROOT' does not exist — cannot resolve $ac's citation"
      AC1_STATUS="FAIL"; continue
    fi

    cls_file="$(find "$TEST_ROOT" -name "$cls.java" -print -quit 2>/dev/null)"
    if [ -z "$cls_file" ]; then
      bad "AC-1: $ac cites $evidence but no $cls.java exists under $TEST_ROOT"
      AC1_STATUS="FAIL"; continue
    fi
    if ! grep -qE "[^A-Za-z0-9_]$mth[[:space:]]*\(" "$cls_file"; then
      bad "AC-1: $ac cites $evidence but $cls_file declares no method $mth"
      AC1_STATUS="FAIL"; continue
    fi

    note "resolved: $ac -> $evidence  (${cls_file})"
  done
fi

[ "$AC1_STATUS" = "PASS" ] \
  && note "AC-1 citation half PASS — every AC resolves to a real test method. Read them before writing the verdict."

# ---------------------------------------------------------------- verdict block

echo
echo "AC-1: $AC1_STATUS"
echo "AC-2: $AC2_STATUS"
echo "BUILD-2: $BUILD2_STATUS"

if [ ${#fail_lines[@]} -gt 0 ]; then
  echo
  printf '%d check(s) failed. Quote the failing line verbatim in ## Gate results.\n' "${#fail_lines[@]}"
  exit 1
fi
exit 0
