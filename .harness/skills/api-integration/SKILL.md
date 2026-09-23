# api-integration

**Purpose.** The wire contract StoreOps endpoints honour, and the event bus mechanics that keep
cross-module side effects legal under ARCH-2. Read by the Generator only.

## URL and method conventions

- Base path `/api`, plural module nouns: `/api/activities`, `/api/programmes`, `/api/alerts`,
  `/api/reports`.
- Path variables are `{id}`, never `{taskId}` in a path that already names the resource.
- Collection sub-actions use a kebab-case segment: `/api/activities/bulk-status`,
  `/api/programmes/{id}/templates`. No verbs in paths beyond that pattern, no `/do-` prefixes.
- `PATCH` for partial update, `PUT` never (there is no full-replace semantics anywhere in StoreOps).
- Filters are query parameters, always optional, never required:
  `GET /api/activities?programmeId=&status=&category=`.

## Status codes

| Status | When |
|---|---|
| 200 | Successful read, or a write where every item succeeded |
| 201 | Resource created; `Location` header set |
| 207 | Bulk operation where at least one item succeeded and at least one failed |
| 400 | Bean Validation failure, malformed body |
| 401 | Missing or expired `AuthToken` |
| 403 | Authenticated but wrong `StaffRole` |
| 404 | Id does not resolve |
| 409 | Conflict — duplicate, concurrent modification |
| 422 | Business rule refused a well-formed request |

The 400/422 split is load-bearing and gets confused constantly. 400 means "I cannot read this".
422 means "I read it, it is well-formed, and the domain says no". A bulk request with an empty
`updates` array is 400. A bulk request asking to move a `DONE` activity to `BLOCKED` is 422 — or, if
other items in the same request succeeded, 207 with that item in `failed[]`.

**A whole-request 422 and a per-item failure are different outcomes.** Decide which applies before
writing the endpoint: if any well-formed item can independently succeed, the endpoint is partial-success
and returns 207. If the request only makes sense as a unit, it is all-or-nothing and returns 422.
Never mix the two in one endpoint.

## Response envelopes

Success responses return the resource or result directly. No `{ "data": … }` wrapper.

Errors are uniform, produced by `GlobalExceptionHandler`, and never hand-built in a controller:

```json
{
  "code": "INVALID_TRANSITION",
  "message": "Activity a-1042 cannot move from DONE to BLOCKED",
  "timestamp": "2026-09-22T09:14:03Z",
  "path": "/api/activities/bulk-status"
}
```

Bean Validation failures add a `fieldErrors` array. No stack traces, no `cause` chains, no internal
class names — `GlobalExceptionHandler` maps anything that is not an `AppException` to
`code: "INTERNAL_ERROR"` with a generic message and logs the real exception at `ERROR`.

Bulk results:

```json
{
  "requestedCount": 4,
  "succeededCount": 3,
  "failedCount": 1,
  "succeeded": [
    { "taskId": "a-1041", "previousStatus": "IN_PROGRESS", "newStatus": "DONE" }
  ],
  "failed": [
    { "taskId": "a-1042", "reason": "INVALID_TRANSITION" }
  ]
}
```

Both arrays are always present, empty rather than null or omitted. `reason` uses the same code
vocabulary as `AppException.code()`, so a client handles a per-item failure with the same branch it
uses for a whole-request failure. Counts are included even though they are derivable, because the
handheld client renders the summary before it renders the lists.

Item order in `succeeded`/`failed` matches request order. A client showing a list needs to line
results up against what the user submitted.

### Code vocabulary

One table, because these strings are the API contract. Clients branch on them, an acceptance
criterion names them literally, and a `reason` in `failed[]` and a `code` in an error body draw from
the same list. Never invent a code without a line in the sprint contract.

| Code | Status when whole-request | Meaning |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation rejected the body; `fieldErrors` is populated |
| `UNAUTHORIZED` | 401 | Missing or expired `AuthToken` |
| `FORBIDDEN` | 403 | Caller's `StaffRole` or store scope does not permit this item |
| `NOT_FOUND` | 404 | Id does not resolve |
| `CONFLICT` | 409 | Duplicate, or concurrent modification |
| `INVALID_TRANSITION` | 422 | The current status may not move to the target status |
| `ALREADY_IN_STATUS` | 422 | Target status equals current status — idempotent repeat |
| `UNSUPPORTED_TARGET_STATUS` | 422 | Target is not `DONE` or `BLOCKED`; handover marks work finished or stuck |
| `INTERNAL_ERROR` | 500 | Anything `GlobalExceptionHandler` did not recognise; generic message, no stack trace |

`FORBIDDEN`, `NOT_FOUND`, `INVALID_TRANSITION`, `ALREADY_IN_STATUS` and
`UNSUPPORTED_TARGET_STATUS` are the five that can appear as a per-item `reason`. The others only ever
fail a whole request.

### Request limits

Bulk endpoints are bounded, and the bound is a shape constraint rather than a business rule, so it
lives on the DTO as Bean Validation and fails 400:

| Field | Limit |
|---|---|
| `updates` | non-empty, at most 200 items |
| `updates[].taskId` | non-blank |
| `updates[].targetStatus` | non-null; `DONE` or `BLOCKED` is checked in the service, not here |
| `updates[].note` | optional, at most 500 characters |

200 is the cap a handheld shift-close can produce in one screen-full of selections, and it bounds the
repository round trip. A request of 201 items is a 400 with `VALIDATION_FAILED`, not a truncated 207.

## Idempotency

Bulk state transitions are idempotent by outcome: submitting the same `taskId` with the same
`targetStatus` twice leaves the activity in that status, and the second attempt reports the item under
`failed` with reason `ALREADY_IN_STATUS` rather than creating a second audit entry. Mark it failed,
not succeeded — a `succeeded` entry implies a state change and an audit row, and reporting one that
did not happen corrupts the handover record.

## Auth

`AuthToken` in `Authorization: Bearer <token>`, resolved to a `StoreOpsUser` and injected with
`@AuthenticationPrincipal`. Controllers never parse the header themselves.

Role checks: enforce in the service, not the controller, because role rules are business rules and
belong under ARCH-3's service layer. The service throws `ForbiddenException`.

Scoping rules that apply to every endpoint:

- `ASSOCIATE` and `DEPARTMENT_LEAD` see and act on their own `storeId` only
- `STORE_MANAGER` acts across their store, including activities assigned to others
- `REGIONAL_MANAGER` reads across the region; writes are still store-scoped
- Any endpoint returning data from more than one store must be `REGIONAL_MANAGER` only

A bulk request containing activity ids from a store the caller cannot act on does not 403 the whole
request. Those items land in `failed[]` with reason `FORBIDDEN`. A caller with no access to *any* item
in the request gets a whole-request 403 — an empty success set with an all-forbidden failure set is
indistinguishable from an attack probe and should not return 207.

## Event bus

The only legal route for a cross-module side effect. Direct service calls into another module for a
write fail gate ARCH-2.

```java
public interface StoreOpsEvent {
    String eventType();
    Instant occurredAt();
}

public interface StoreOpsEventBus {
    void publish(StoreOpsEvent event);
}
```

The implementation in `shared.event` wraps Spring's `ApplicationEventPublisher`. Do not inject
`ApplicationEventPublisher` directly into a module — always the `StoreOpsEventBus` interface, because
ArchUnit's ARCH-2 rule watches that type and a direct publisher injection routes around the thing the
gate inspects.

Publishing, from `activities`:

```java
@Service
public class ActivityServiceImpl implements ActivityService {

    private final TaskRepository taskRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final StoreOpsEventBus eventBus;

    // constructor omitted

    @Override
    public BulkStatusResult bulkUpdateStatus(BulkStatusCommand command) {
        BulkStatusResult result = applyTransitions(command);

        if (!result.succeeded().isEmpty()) {
            eventBus.publish(new ShiftHandoverCompletedEvent(
                    command.storeId(),
                    command.actingUserId(),
                    result.succeeded().stream().map(UpdatedItem::taskId).toList(),
                    Instant.now()));
        }
        return result;
    }
}
```

Three things that example is asserting:

1. Publish **after** the state change, from the service, once per operation. Not per item, not from
   the controller, not from the repository.
2. Do not publish when nothing changed. An event means something happened.
3. `activities` has no dependency on `alerts`. It does not know a notification results from this.

Consuming, in `alerts`:

```java
@Component
public class ShiftHandoverListener {

    private final NotificationService notificationService;

    @EventListener
    public void on(ShiftHandoverCompletedEvent event) {
        notificationService.create(new CreateNotificationCommand(
                resolveRecipient(event.storeId()),
                AlertType.SHIFT_HANDOVER,
                NotificationChannel.IN_APP,
                payloadFrom(event)));
    }
}
```

The listener lives in the **consuming** module. `alerts` may depend on `alerts.service` freely; that
is not a cross-module call. Listeners go in `<module>/service/listener/` or `<module>/listener/`,
never in `web`.

### Event catalogue

| Event | Published by | Consumed by | Payload |
|---|---|---|---|
| `SlaBreachedEvent` | activities | alerts | taskId, assigneeId, priority, dueDate, storeId |
| `ShiftHandoverCompletedEvent` | activities | alerts | storeId, handoverByUserId, updatedTaskIds, occurredAt |
| `ProgrammeClosedEvent` | programmes | reports, alerts | programmeId, storeId, closedByUserId |
| `TaskBlockedEvent` | activities | alerts | taskId, blockedByUserId, reason |
| `RegionalRollupRequestedEvent` | reports | reports | regionId, requestedByUserId |

Adding an event means adding a row here. An event with no consumer is legal and sometimes correct
(audit, future use) — note it in `Known gaps` so the Evaluator does not read it as a dangling wire.

### What the event bus is not for

- **Not for reads.** Needing data from another module means calling its service synchronously — and
  never into `alerts` or `reports`, which are event-only in both directions under ARCH-2. An event
  that expects a reply is a request, and this bus has no reply channel.
- **Not for same-module decoupling.** Inside `activities`, call the method. The one exception in the
  catalogue is `RegionalRollupRequestedEvent`, published and consumed by `reports`: a rollup is
  deferred work, and the event is the record that it was requested, not an indirection between two
  `reports` classes that could have called each other. If a sprint contract asks for a same-module
  event, it must say why in the same sentence; otherwise call the method.
- **Not a transaction boundary.** Delivery is synchronous and in-process. If a listener throws, it
  surfaces in the publisher's call stack. Listeners must not throw for anything the publisher cannot
  act on — catch, log, and set the `Notification` status to `FAILED` instead. A bulk handover must not
  fail because one notification could not be created.
