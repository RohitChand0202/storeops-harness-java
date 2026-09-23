# Template — `sprint-N-contract.md`

Written by the Planner to `.harness/output/`. The Evaluator parses the header and the AC numbering,
so both are fixed format.

> This file is the **reference shape**, worked through for sprint 1 of the shift handover feature so
> the format is unambiguous. Real contracts are produced by the Planner into `.harness/output/` and
> archived to `.harness/reviews/`. This file is never run output and is never copied there —
> `verify-submission.sh` fails the submission if a `TEMPLATE` file appears in the archive.

---

```
SPRINT: 1
TITLE: Bulk status transition — service layer
DEPENDS_ON: NONE
MODULES_TOUCHED: activities
LAYERS: domain, repository, service, tests
```

`MODULES_TOUCHED` and `LAYERS` are parsed by `.harness/checks/ac-gates.sh` to decide gate `AC-2`.
A contract missing either field makes the gate undecidable, which is recorded `FAIL`.

## Deliverables

- A service operation that accepts a list of activity ids with a target status and returns a result
  carrying both the transitions that succeeded and the ones that did not, with a reason per failure.
- Transition validation: only `TODO` and `IN_PROGRESS` may move to `DONE` or `BLOCKED`.
- An audit entry per successful transition, recording the previous status, the new status, the acting
  user, and the time.
- Unit tests covering every acceptance criterion below.

Expressed as behaviour, not file paths. The Generator chooses class names from
`coding-conventions`; the contract does not.

## Acceptance criteria

```
AC-1.1
GIVEN three activities with status IN_PROGRESS in the caller's store
WHEN bulkUpdateStatus is called with all three ids and target status DONE
THEN the result's succeeded[] contains all three ids
AND each activity's status in the repository is DONE
AND the result's failed[] is empty
```

```
AC-1.2
GIVEN an activity with status DONE
WHEN bulkUpdateStatus is called with that activity id and target status BLOCKED
THEN the result's failed[] contains an entry for that id with reason INVALID_TRANSITION
AND the activity's status in the repository is still DONE
AND no audit entry is created for that id
AND the result's succeeded[] does not contain that id
```

```
AC-1.3
GIVEN a request containing two valid activity ids and one id that does not exist
WHEN bulkUpdateStatus is called
THEN the two valid activities transition and appear in succeeded[]
AND the unknown id appears in failed[] with reason NOT_FOUND
AND the operation does not throw
```

```
AC-1.4
GIVEN an activity that transitions successfully
WHEN bulkUpdateStatus completes
THEN exactly one audit entry exists for that activity id
AND the audit entry records previousStatus, newStatus, and the acting user id
```

```
AC-1.5
GIVEN a request in which every item fails validation
WHEN bulkUpdateStatus is called
THEN succeeded[] is empty
AND no audit entry is created for any id in the request
AND no call is made to taskRepository.save
```

```
AC-1.6
GIVEN an activity already in status DONE
WHEN bulkUpdateStatus is called with target status DONE for that id
THEN the item appears in failed[] with reason ALREADY_IN_STATUS
AND no second audit entry is created for that id
```

Every THEN names an observable — a result-collection membership, a persisted field value, a count, or
a repository interaction. Each rule carries at least one negative clause, because the characteristic
bulk-endpoint defect is doing the work *and also* something extra.

## Out of scope for this sprint

- No HTTP layer. No controller, no DTO, no request validation annotations.
- No event publication. `ShiftHandoverCompletedEvent` is sprint 3.
- No change to single-activity `PATCH /api/activities/{id}` behaviour.
- No transitions to `TODO` or `IN_PROGRESS` — handover marks work finished or stuck, only.
- No bulk delete.

## Architecture notes

- ARCH-3 applies: this sprint is service and below. A file under `activities/web/` in this diff is an
  `AC-2` gate failure.
- ERR-1 applies: partial failures are reported in the result, never thrown. A whole-request failure
  (caller has access to no item in the request) throws `ForbiddenException`.
- ARCH-2 does **not** apply yet — nothing in this sprint crosses a module boundary. Do not
  pre-emptively add an event bus dependency.

## Out-of-band risks

None. The feature needs no new dependency, no schema change, and no change to auth handling.
