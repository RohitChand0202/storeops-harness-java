# sprint-decomposition

**Purpose.** How to split a StoreOps feature into sprint contracts, and how to write acceptance
criteria that an Evaluator can grade without judgement. Read by the Planner only.

## Where to draw a sprint boundary

Boundaries go at **risk seams**, not at layer seams. The point of a sprint boundary is that a FAIL on
one side cannot corrupt passing work on the other.

Three rules, in priority order:

**1. Separate the business-rule sprint from the HTTP sprint.** The service layer holds the logic the
Evaluator's LLM-assessed checks care about; the web layer holds DTO shape and validation, which the
automated gates catch. Mixing them means a retry caused by a DTO field name churns the service code
that already passed.

**2. Put every cross-module event emission in its own sprint, after the module's own logic passes.**
ARCH-2 is the rule the Generator is most likely to break, and a FAIL on ARCH-2 usually means rewriting
the call site. Isolating it keeps that rewrite away from the transaction semantics.

**3. Never split a transaction boundary across sprints.** If sprint 1 writes half a state change and
sprint 2 writes the other half, sprint 1 cannot pass — its ACs describe a system that is inconsistent
at rest. Split by *capability*, not by *field*.

Two or three sprints for a single-endpoint feature. Four is a warning sign. Five means the feature
needs splitting at the developer level — say so in `Out-of-band risks` rather than planning it.

### Worked example — shift handover bulk update

The feature: `PATCH /api/activities/bulk-status`, letting outgoing shift staff mark multiple
activities `DONE` or `BLOCKED` in one request, with partial failure handling and an audit entry per
updated activity.

| Sprint | Delivers | Why the boundary is here |
|---|---|---|
| 1 | Service-layer bulk transition with per-item outcome, transition validation, audit entry per success | This is the only genuinely hard part: per-item success/failure semantics inside one call. It is fully testable with no HTTP. If it FAILs, nothing else has been built on top of it yet. |
| 2 | `web` layer — DTO, Bean Validation, controller, `207` partial-success response mapping | Depends on sprint 1's result type existing. Isolated so that a DTO field rename does not touch validated service logic. Automated gates carry most of the weight here. |
| 3 | `ShiftHandoverCompletedEvent` published once per bulk operation; `alerts` listener creates a `SHIFT_HANDOVER` notification for the incoming shift's department lead | ARCH-2 is at maximum risk here. Isolating the emission means an ARCH-2 FAIL retries ~40 lines, not the whole feature. Also the only sprint that touches two modules. |

Note what is *not* a sprint: "write the tests". Tests ship with the sprint whose behaviour they prove.
A test-only sprint lets the Generator claim an AC satisfied in sprint 1 and then satisfy it in
sprint 3, which defeats the gate.

## Acceptance criteria

Format: GIVEN / WHEN / THEN, numbered `AC-<sprint>.<n>`.

The rule that makes a criterion gradeable: **every THEN must name an observable.** Exactly one of:

- a returned HTTP status and a named response body field
- a persisted field's value after the call
- an `AppException` subclass and its `code`
- a published event type and a named payload field
- a count (how many rows changed, how many notifications created)

If a THEN names none of these, it is a wish, not a criterion. Rewrite it.

### Examples

Good:

```
AC-1.2
GIVEN an activity with status DONE
WHEN bulkUpdateStatus is called with that activity id and target status BLOCKED
THEN the result's failed[] contains an entry for that id with reason INVALID_TRANSITION
AND the activity's status in the repository is still DONE
AND no audit entry is created for that id
AND the result's succeeded[] does not contain that id
```

Four clauses, four observables, one test. Every clause is checkable and three of them are negative.
The third matters most: without it, a Generator can satisfy the criterion while writing an audit
entry for a failed item, which is exactly the kind of bug a bulk endpoint ships with. The fourth
closes the remaining hole — reporting the same id as both failed and succeeded.

Keep the clause count stable once a contract is archived. The Evaluator's `AC-1` verdict is "does the
cited test assert all four", which is a count, and a count is only deterministic if the criterion it
counts against does not drift.

Also good, for an event:

```
AC-3.1
GIVEN a bulk update in which at least one activity transitioned successfully
WHEN the service completes
THEN exactly one ShiftHandoverCompletedEvent is published
AND its payload contains storeId, handoverByUserId, and updatedTaskIds matching the succeeded ids
AND no call is made to alerts.service from the activities module
```

The last clause makes ARCH-2 part of the acceptance criteria rather than only a gate. It costs one
line and converts an architecture rule into something the Generator is told to satisfy up front,
which is cheaper than catching it on review.

Bad, and why:

| Bad AC | Problem |
|---|---|
| THEN the endpoint handles errors gracefully | No observable. Cannot fail. |
| THEN the response is a 200 | Status-only assertion — this is failure mode 3 in AC form. Pair every status with a body or state assertion. |
| THEN the code follows project conventions | That is a gate, not a criterion. Never restate a gate as an AC. |
| THEN performance is acceptable for bulk operations | Unbounded. If throughput matters, name a limit: "a request of 200 items completes in one repository round trip". |
| THEN the service notifies the department lead | Ambiguous between a call and an event — and the Generator will choose the call. Name the event. |

## Scope boundary section

Required in every spec. Write what the feature explicitly does not do, focusing on the adjacent thing
a reasonable implementer would assume was included. For the bulk update feature:

- does not change single-activity `PATCH` behaviour
- does not add a bulk *delete*
- does not allow transitions to `TODO` or `IN_PROGRESS` — handover marks work finished or stuck, only
- does not backfill audit entries for activities updated before this feature shipped
- does not notify assignees individually; one handover notification per bulk operation

Each line prevents a specific plausible over-build. Scope creep in generated code is nearly always the
implementer answering a question the spec left open.

## Architecture notes in a contract

Only add a note where the sprint is at genuine risk of tripping a gate, and always name the gate ID.
A contract that lists all five rules on every sprint trains the Generator to skim them.

Good: `Sprint 3 writes into alerts. ARCH-2 applies: publish ShiftHandoverCompletedEvent; the listener
lives in alerts. Do not inject NotificationService.`

Bad: `Follow all architecture principles.`

## Out-of-band risks

Anything requiring a decision the harness may not make. Populate this honestly — an empty list on a
feature that needs a new dependency is how a run reaches escalation on iteration 3 for a reason
nobody could have fixed inside the loop. Typical entries: a new Maven dependency, a change to
`AuthToken` handling, anything touching `staff` writes, anything that would make `reports` non-read-only,
a coverage threshold that the sprint cannot plausibly meet.
