SPRINT: 3
TITLE: Handover event and alert delivery
DEPENDS_ON: 2
MODULES_TOUCHED: activities, alerts, staff, shared
LAYERS: repository, service, events, tests

## Deliverables

- A completed bulk operation with one or more successes publishes one `ShiftHandoverCompletedEvent` through `StoreOpsEventBus`, containing the store, authenticated actor, successful activity ids, and completion time.
- The staff module exposes a read-only lookup of active department leads in a store.
- An alerts listener consumes the event and creates one `SHIFT_HANDOVER` in-app notification for an eligible lead other than the outgoing actor.
- An absent eligible lead or alert-delivery failure does not reverse completed activity transitions or audit entries.

## Acceptance criteria

| ID | GIVEN | WHEN | THEN |
| --- | --- | --- | --- |
| AC-3.1 | A bulk request has at least one successful transition, with or without rejected items | The activities service completes the bulk operation | Exactly one `ShiftHandoverCompletedEvent` is published through `StoreOpsEventBus`; its `storeId` and `handoverByUserId` match the authenticated actor, `updatedTaskIds` matches only `succeeded[]` ids in order, and `occurredAt` is the operation time. |
| AC-3.2 | Every requested item is rejected without a transition | The activities service returns its result | No `ShiftHandoverCompletedEvent` is published and no audit entry is appended for the rejected ids. |
| AC-3.3 | Staff records include active and inactive leads, other roles, and more than one store | The staff service looks up department leads for store S | The returned ids contain only active `DEPARTMENT_LEAD` users whose `storeId` is S; the stored `User` records are unchanged. |
| AC-3.4 | A handover event has two updated ids and exactly one eligible active department lead in the event's store other than the outgoing actor | The alerts listener consumes the event | Exactly one notification is persisted for that lead with `alertType=SHIFT_HANDOVER`, `channel=IN_APP`, and a payload containing the store id and count 2. |
| AC-3.5 | No active department lead other than the outgoing actor is available in the event's store | The alerts listener consumes the event | Zero notifications are created for that event; the previously persisted activity statuses and audit-entry counts remain unchanged. |
| AC-3.6 | An eligible lead exists but alert creation fails | The alerts listener consumes an event after successful activity updates | No listener exception escapes to the bulk caller; the bulk result still reports the successful ids and their stored statuses and audit-entry counts remain committed. |

## Out of scope for this sprint

- No changes to the HTTP request or response shape, single-activity PATCH, or transition rules.
- No write to `staff`, no incoming-shift roster, and no individual alert for every changed activity or assignee.
- No per-activity `TaskBlockedEvent` emission from this bulk operation; the handover event represents the batch.

## Architecture notes

- ARCH-2: `activities` publishes through `StoreOpsEventBus`; only `alerts` calls its own notification service. No activities class imports or invokes `alerts.service`.
- ARCH-1: the alerts listener reads staff through `StaffService`, and only staff code accesses `UserRepository`.
- ARCH-3: the staff lookup is read-only service/repository behavior; event handling and notification creation stay in `alerts.service`.
