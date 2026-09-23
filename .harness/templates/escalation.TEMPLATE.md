# Template — `escalation.md`

Written to `.harness/output/escalation.md` by the orchestrator on the third consecutive FAIL of the
same sprint. Per sprint, not per run — a sprint needing three attempts and a run needing nine are
different problems.

---

```
SPRINT: 2
FEATURE: Shift handover bulk update — PATCH /api/activities/bulk-status
ITERATION_COUNT: 3
DATE: 2026-09-22T16:02:00Z
HARD_GATES_FAILED_ALL_ITERATIONS: TEST-1
RECOMMENDED_ACTION: REVISE_CONTRACT
BRANCH: harness/sprint-2-iteration-3
```

## Blocking issue

`TEST-1` failed on all three iterations.

```
Rule violated: com.cognizant.storeops.activities.web
  lines covered ratio is 0.61, but expected minimum is 0.70
```

The controller's 207-versus-200 branch and its four validation-failure paths are reachable only
through request shapes the sprint contract does not specify, so the Generator has no acceptance
criterion to write a test against.

## What changed between iterations

| Iteration | Change | Result |
|---|---|---|
| 1 | Controller, DTO, 200/207 mapping, 3 MockMvc tests | web coverage 0.54 |
| 2 | Added 2 MockMvc tests for validation failures | web coverage 0.61 |
| 3 | Added a test for the empty-updates case | web coverage 0.61 — new test hit an already-covered path |

This field is the one that earns its place: iteration 3 adding coverage that did not move the number
says the Generator has exhausted the paths the contract describes. That is a contract problem, not a
Generator problem, and it needs a different human response from a Generator looping on one mistake.

## Files touched across all iterations

- `src/main/java/.../activities/web/ActivityController.java`
- `src/main/java/.../activities/web/BulkStatusUpdateRequest.java`
- `src/main/java/.../activities/web/BulkStatusUpdateResponse.java`
- `src/test/java/.../activities/web/ActivityControllerTest.java`

## Recommended action

`REVISE_CONTRACT`. Sprint 2's ACs cover the success path and two validation failures. The web
coverage threshold requires the remaining validation branches, which means ACs for: an `updates`
array exceeding 200 items, a null `targetStatus`, a blank `taskId`, and a `note` over 500 characters.
Add four ACs and re-run sprint 2. Do not lower the threshold — it is 70% for every module and this
sprint is not special.

## Recipient

The developer who invoked the run. Not copied to the engineering standards team: the failing gate is
`TEST-1`, not `ARCH-1`, `ARCH-2`, or `ERR-1`. Those three exist for the standards team's own stated
concerns, and a repeated failure of one of them is evidence their rule needs their attention.

## Expected response

Within one working day: amend the contract, amend a skill file, or implement by hand and log the
harness gap in `debt-log.md`. What must not happen is waiving the gate for this sprint. If a gate is
wrong it is wrong for every sprint, and it gets changed in the open.
