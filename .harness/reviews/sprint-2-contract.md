SPRINT: 2
TITLE: Bulk status HTTP contract
DEPENDS_ON: 1
MODULES_TOUCHED: activities
LAYERS: web, tests

## Deliverables

- `PATCH /api/activities/bulk-status` accepts `updates[]` entries containing `taskId`, `targetStatus`, and optional `note`.
- A valid request has 1 to 200 entries; each entry has a nonblank id, nonnull target, and note of at most 500 characters.
- The response reports `requestedCount`, `succeededCount`, `failedCount`, `succeeded[]`, and `failed[]` with the service's per-item outcomes.
- The controller passes the authenticated principal to the service; it does not use a caller-supplied actor id.

## Acceptance criteria

| ID | GIVEN | WHEN | THEN |
| --- | --- | --- | --- |
| AC-2.1 | A valid bearer principal and a service result with one success and no failure | The client sends a valid bulk-status PATCH | HTTP status is 200; `requestedCount=1`, `succeededCount=1`, `failedCount=0`; `succeeded[0]` contains `taskId`, `previousStatus`, and `newStatus`; `failed[]` is empty. |
| AC-2.2 | A valid bearer principal and a service result with one success and one `NOT_FOUND` failure | The client sends both updates in one PATCH | HTTP status is 207; `requestedCount=2`, `succeededCount=1`, `failedCount=1`; `succeeded[0].taskId` is the applied id; `failed[0]` contains the rejected `taskId` and `reason=NOT_FOUND`. |
| AC-2.3 | A valid bearer principal and a service result in which every item failed | The client sends the bulk-status PATCH | HTTP status is 207; `succeededCount=0`, `failedCount=requestedCount`, and each `failed[]` entry contains its submitted `taskId` and service-provided `reason`. |
| AC-2.4 | A valid bearer principal | The PATCH body has an empty `updates[]`, more than 200 entries, a blank `taskId`, a null `targetStatus`, or a note longer than 500 characters | Each invalid body returns HTTP 400 with `code=VALIDATION_FAILED`; the activities service is not called for that body. |
| AC-2.5 | No bearer token or an invalid bearer token | The client sends an otherwise valid bulk-status PATCH | HTTP status is 401 with `code=UNAUTHORIZED`; the activities service is not called. |
| AC-2.6 | A valid token resolves to actor A, and the JSON body also includes an actor field naming B | The client sends a valid bulk-status PATCH | The service receives actor id A from the resolved principal; the response outcome is derived from the service result, and no service command uses B as the actor. |

## Out of scope for this sprint

- No change to sprint 1's transition, audit, or per-item failure rules.
- No event publication, staff lookup, or alert creation; these are sprint 3.
- No change to the shared bearer-token resolver or the authentication scheme. This endpoint consumes the existing principal.

## Architecture notes

- ARCH-3: the controller maps request/response fields and HTTP status only. The activities service owns every transition, store-access, and per-item outcome decision.
