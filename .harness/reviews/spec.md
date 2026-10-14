STATUS: AWAITING APPROVAL
FEATURE: Shift handover bulk status update through PATCH /api/activities/bulk-status
SPRINTS: 3
MODULES_TOUCHED: activities, alerts, staff, shared
EVENTS_EMITTED: ShiftHandoverCompletedEvent
OUT_OF_BAND_RISK: The current staff model has no incoming-shift assignment; the demo uses an eligible department lead in the same store.

## Intent

An authenticated outgoing store staff member can submit up to 200 activity status changes in one handover request. Each item independently reaches `DONE` or `BLOCKED`, or returns a stable failure reason without preventing valid items from succeeding. Each applied transition has one append-only audit entry attributed to the authenticated caller. A completed handover with at least one successful transition raises one event for an in-app alert to an eligible department lead.

## Scope boundary

- This feature does not change the existing single-activity `PATCH /api/activities/{id}` behavior, add bulk delete, or backfill earlier audit records.
- Handover targets are `DONE` and `BLOCKED` only. It does not reopen work into `TODO` or `IN_PROGRESS`.
- The request cannot select an acting user or a different store. The bearer token resolves the actor and the actor's store.
- Per-item business failures do not roll back other valid items. An invalid request body is rejected as a whole. A request whose existing activities are all in another store is rejected as a whole; unknown ids return per-item `NOT_FOUND` outcomes, including when every id is unknown.
- The feature does not notify every assignee, emit one alert per activity, or add a reports workflow.
- No database, shift roster, authentication scheme, or new dependency is introduced.

## Module impact

| Module | Impact |
| --- | --- |
| `activities` | Service and repository-level writes to `Task` and append-only `AuditEntry`; web-layer request validation and response mapping. The service publishes the handover event after its own writes. |
| `shared` | Defines the `ShiftHandoverCompletedEvent` payload on the existing `StoreOpsEventBus`; no business or persistence logic. Existing bearer-token authentication supplies a `StoreOpsUser` principal. |
| `staff` | Read-only service/repository lookup of active department leads for one store. The listener never writes a `User`. |
| `alerts` | Consumes the event and writes one `SHIFT_HANDOVER` in-app notification through its own service. |

The only cross-module side effect is `activities` publishing `ShiftHandoverCompletedEvent` for `alerts` to consume. `activities` never calls `alerts.service` or its repository.

## Sprint breakdown

**Sprint 1 — service transition and audit.** Implement the independent per-item outcomes, legal transition checks, store access, and one audit entry for each applied transition. This is testable without HTTP and keeps the status and audit write together as one capability.

**Sprint 2 — HTTP contract.** Expose `PATCH /api/activities/bulk-status` using a bounded, validated request and a response with ordered success and failure lists plus counts. Map complete success to 200 and any per-item failure to 207. The route uses the principal established by the existing bearer-token infrastructure.

**Sprint 3 — handover event and alert.** Publish one event containing only successful ids after the activities operation completes. Add the read-only staff lookup and an `alerts` listener that creates one in-app alert for an eligible department lead. This isolates the cross-module event boundary from the transition rules and HTTP shape.

## Out-of-band risks

The staff model does not identify who is on the incoming shift. For this demonstration, “incoming department lead” means one active `DEPARTMENT_LEAD` in the event's store other than the outgoing actor; if none exists, no alert is written and the activity results remain committed. Production delivery to the actual incoming shift requires a roster or assignment decision outside this feature. No new dependency, schema, or authentication model change is needed for the demonstration.
