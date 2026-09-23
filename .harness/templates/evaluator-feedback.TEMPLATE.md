# Template — `evaluator-feedback.md`

Written by the Evaluator to `.harness/output/`, archived to `.harness/reviews/`. The first block is
parsed by the orchestrator: field order is fixed, and an unparseable block is routed as a FAIL.

---

```
VERDICT: FAIL
SPRINT: 1
ITERATION: 1
HARD_GATES_FAILED: AC-1, TEST-2
SCORE: 0
```

`HARD_GATES_FAILED` lists every gate whose result is FAIL in the table below — all of them, not the
most important one. The Monitor copies this field into `GATES_FAILED_BY_ITERATION` in the run log,
so a gate omitted here disappears from the archive, and the Monitor's `## Skill file signal` depends
on counting the same gate across sprints. `AC-1` and `TEST-2` failing together is the normal
pairing: a test that does not assert an AC's observables fails both by construction.

## Gate results

| Gate | Result | Deciding output |
|---|---|---|
| BUILD-1 | PASS | `./mvnw -q clean verify -Pharness-gates` exit 0 |
| BUILD-2 | PASS | Checkstyle 0 violations; `ac-gates.sh` — `BUILD-2: PASS` |
| ARCH-1 | PASS | `repositoriesAreModulePrivate` — 0 violations |
| ARCH-2 | PASS | `alertsIsReachedByEventOnly` — 0 violations |
| ARCH-3 | PASS | `layersAreRespected` — 0 violations |
| ARCH-4 | PASS | `reportsNeverWrite` — 0 violations |
| ERR-1 | PASS | Checkstyle 0 violations; `servicesThrowOnlyAppExceptions` — 0 violations |
| TEST-1 | PASS | `jacoco:check` — service 84%, overall 78% |
| TEST-2 | FAIL | AC-1.2's cited test asserts `failed()` non-empty only — no state or outcome assertion |
| AC-1 | FAIL | `ac-gates.sh` — `AC-1: PASS` (citations resolve); read half fails: AC-1.2 and AC-1.5 cited tests do not assert their observables |
| AC-2 | PASS | `ac-gates.sh` — `AC-2: PASS`, 4 files in activities/{service,domain} |

Quote the tool. `alertsIsReachedByEventOnly — 0 violations` is a gate result; "boundaries look fine"
is not. Where a gate has an automated half and a read half, record both — the `AC-1` row above shows
the script passing and the reading failing, which is the distinction the Generator needs in order to
know that the problem is the assertions and not a missing test.

## Dimension scores

Omitted — hard gate failure. No dimension scoring runs when a gate fails.

## AC verification

| AC | Status | Note |
|---|---|---|
| AC-1.1 | satisfied | `marksEligibleActivitiesDone` asserts succeeded[] membership and persisted status |
| AC-1.2 | not satisfied | cited test asserts `failed()` non-empty only; 3 of 4 observables unasserted |
| AC-1.5 | not satisfied | cited test asserts succeeded count only; no audit or save assertion |
| AC-1.6 | not satisfied | `NOT IMPLEMENTED`, declared in Known gaps |

## Required fixes

1. `src/test/java/.../BulkStatusServiceTest.java:41` — `rejectsTransitionFromDone` asserts only that
   `failed()` is non-empty. AC-1.2 names four observables. Add: `failed().get(0).reason()` equals
   `INVALID_TRANSITION`; the task's status after the call is still `DONE`;
   `verify(auditEntryRepository, never()).save(any())`; `succeeded()` does not contain the id.
   Gate TEST-2, gate AC-1.

2. `src/test/java/.../BulkStatusServiceTest.java:63` — `persistsNothingWhenEveryItemFails` asserts
   the succeeded count is zero. AC-1.5 also requires no audit entry for any id and no call to
   `taskRepository.save`. Add both `never()` verifications. Gate AC-1.

3. AC-1.6 is unimplemented. The contract's THEN is explicit — the item appears in `failed[]` with
   reason `ALREADY_IN_STATUS` and no second audit entry is created. The Known gaps note asks a
   question the contract already answers. Implement as specified. Gate AC-1.

Every fix carries `path:line`, the rule or AC violated, and what correct looks like. This section is
the Generator's entire input on retry — a fix it cannot act on without asking a human is a defect in
the review, not in the code.

## Advisory

- `BulkStatusResult` exposes mutable `List` fields. `List.copyOf` in the constructor would make the
  record genuinely immutable. Non-blocking, not scored.
- The field-injection inconsistency noted in Known gaps is real and pre-dates this sprint. Belongs in
  a refactor spec, not this verdict.
