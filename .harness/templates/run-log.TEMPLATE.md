# Template — `sprint-N-run-log.md`

Written by the Monitor directly to `.harness/reviews/` and committed. Fields are fixed so the archive
stays sortable across dozens of sprints — never write `N/A`, use `0`, `NONE`, or `unknown`.

---

```
SPRINT: 1
FEATURE: Shift handover bulk update — PATCH /api/activities/bulk-status
DATE: 2026-09-22T14:31:00Z
VERDICT: PASS
ITERATIONS_USED: 2
ESCALATED: false
GATES_FAILED_BY_ITERATION:
  1: AC-1, TEST-2
  2: NONE
  3: NONE
FINAL_SCORE: 93
MODULES_TOUCHED: activities
FILES_CHANGED: 5
TESTS_ADDED: 7
COVERAGE_SERVICE: 86
COVERAGE_OVERALL: 79
TOKEN_COST_ESTIMATE: ~74,000 (estimated)
```

## Quality trend

First sprint of this run; no prior logs to compare. Iteration 1 failed on AC-1 and TEST-2 together,
which is the expected pairing — a test that does not assert an AC's observables fails both gates by
construction. Coverage sits 6 points above the service threshold, which is enough margin that a
single new branch will not trip it.

Report what the numbers show. Not: opinions about the code, or a guess at why.

## Skill file signal

No signal. A signal requires the same gate ID failing on the **first** iteration of three or more
consecutive sprints — that pattern means the Generator is not being told something clearly enough
before it acts, and the fix is upstream in a skill file rather than downstream in a stricter
Evaluator. One first-iteration failure is the loop working.
