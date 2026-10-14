SPRINT: 1
TITLE: Bulk activity transitions and audit
DEPENDS_ON: NONE
MODULES_TOUCHED: activities
LAYERS: domain, repository, service, tests

## Deliverables

- A service operation applies requested activity status changes independently and returns `succeeded[]` and `failed[]` in request order.
- Valid `TODO` and `IN_PROGRESS` activities in the actor's store may move to `DONE` or `BLOCKED`; rejected items retain their prior state.
- Each successful status transition appends exactly one audit entry with the authenticated actor and optional note.
- Tests assert the result, persisted state, and audit count for both successes and failures.

## Acceptance criteria

| ID | GIVEN | WHEN | THEN |
| --- | --- | --- | --- |
| AC-1.1 | A caller in store S and three store-S activities in `TODO`, `IN_PROGRESS`, and `IN_PROGRESS` | The service receives targets `DONE`, `BLOCKED`, and `DONE` in that order | `succeeded[]` contains those three ids in request order with their previous and new statuses; `failed[]` is empty; each stored `Task.status` equals its requested target. |
| AC-1.2 | An activity eligible for transition and an authenticated actor with an optional note | The service applies the transition | Exactly one new audit entry exists for the activity; its `previousStatus`, `newStatus`, `actingUserId`, `note`, and `occurredAt` match the applied transition, actor, note, and operation time. |
| AC-1.3 | A `DONE` activity in the caller's store | The service receives a request to mark it `BLOCKED` | Its id appears only in `failed[]` with `reason=INVALID_TRANSITION`; its stored status remains `DONE`; its audit-entry count does not increase. |
| AC-1.4 | A `TODO` activity in the caller's store | The same id and target `DONE` occur twice in one request | The first item appears once in `succeeded[]`; the second appears in `failed[]` with `reason=ALREADY_IN_STATUS`; the stored status is `DONE` and exactly one audit entry is appended. |
| AC-1.5 | One valid activity and one nonexistent id | Both are submitted in one request | The valid id appears in `succeeded[]` with its status persisted and one audit entry; the unknown id appears in `failed[]` with `reason=NOT_FOUND` and has no audit entry; the service returns a result instead of throwing. |
| AC-1.6 | One valid caller-store activity and one existing activity in another store | Both are submitted in one request | The local id appears in `succeeded[]`; the other-store id appears in `failed[]` with `reason=FORBIDDEN`; the other-store status and audit-entry count are unchanged. |
| AC-1.7 | An activity in the caller's store | Its requested target is `TODO` or `IN_PROGRESS` | The id appears in `failed[]` with `reason=UNSUPPORTED_TARGET_STATUS`; its stored status and audit-entry count are unchanged. |
| AC-1.8 | Every requested id refers to an existing activity in another store | The service receives the request | It throws `ForbiddenException` with `code=FORBIDDEN`; all requested statuses and audit-entry counts remain unchanged. |
| AC-1.9 | A command has no authenticated actor | The service receives the command | It throws `UnauthorizedException` with `code=UNAUTHORIZED`; no requested status or audit-entry count changes. |
| AC-1.10 | All items are rejected by per-item rules | The service processes the request | `succeeded[]` is empty; `failed[]` has one reason per submitted item in request order; no task save or audit append occurs. |

## Out of scope for this sprint

- No controller, wire DTO, HTTP status mapping, or bearer-token infrastructure changes.
- No handover event publication or alert creation; these are sprint 3.
- No change to the single-activity update route, bulk delete, or audit backfill.

## Architecture notes

- ARCH-3: transition and access rules stay in the activities service; the web layer must not implement them.
- ERR-1: a whole-request authentication or authorization failure uses an `AppException` subclass. Per-item business failures are returned in `failed[]`.
