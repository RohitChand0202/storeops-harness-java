# Combined integration run log

SPRINT: 1-3 (one combined integration run)
FEATURE: Bulk shift handover with partial results, audit entries, and an incoming-lead alert
DATE: 2026-10-12
VERDICT: PASS (combined Evaluator review)
ITERATIONS_USED: 2
ESCALATED: false
GATES_FAILED_BY_ITERATION:
  1: BUILD-1 (SpotBugs: seven EI_EXPOSE_REP findings)
  2: NONE
FINAL_SCORE: 92
MODULES_TOUCHED: activities, alerts, shared, staff
FILES_CHANGED: 17
TESTS_ADDED: 30
COVERAGE_SERVICE: 97.24%
COVERAGE_OVERALL: 98.02%
TOKEN_COST_ESTIMATE: unknown

The two iterations are **combined full verification attempts**, not separate Generator/Evaluator cycles or three sprint runs. The first ran 145 tests with zero failures, then SpotBugs failed. The second ran 146 tests with zero failures, passed SpotBugs and Checkstyle, met the JaCoCo checks, and ended `BUILD SUCCESS`. The final acceptance gate reported AC-1, AC-2, and BUILD-2 as PASS. `FILES_CHANGED` counts the 11 main and six test source files in the feature diff against `444b8dc`; `TESTS_ADDED` counts 30 new test methods there. The staff lookup prerequisite was committed earlier in the same assisted session. Coverage is JaCoCo **line** coverage: service and listener packages 282/290, overall 693/707. Runtime token telemetry and the complete input sizes for every agent invocation were unavailable, so a total token estimate cannot be computed reliably.

## Quality trend

There are no earlier archived run logs for a numerical comparison. This combined run ended PASS at 92 after two full verification attempts.

## Skill file signal

No signal.

## Reproducibility

The contract, summary, and feedback are archived in `.harness/reviews/`. The acceptance gate ran on the feature snapshot committed as `439ef6a`, against base `444b8dc`, before review files were archived. For a replay, check out `439ef6a` in an isolated worktree, supply the archived summary as a working input, then run `.harness/checks/ac-gates.sh --contract .harness/reviews/integration-contract.md --summary .harness/output/generator-summary.md --base 444b8dc`. Running it on the later review-archive commit would also scan those non-source files and fail AC-2 by design. The saved Maven evidence is `.harness/output/verify-first-failed.log` and `.harness/output/verify.log`. Docker and standalone Claude Code CLIs were unavailable here, so this record contains no container or standalone-Claude execution evidence.
