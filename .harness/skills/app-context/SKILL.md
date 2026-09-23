# app-context

**Purpose.** Everything an agent needs to know about what StoreOps *is* before it plans, writes, or
reviews anything. Read by all four agents. This file contains no rules — rules are in
`architecture-principles`. It contains facts.

## The product

StoreOps is a REST API for retail store operations. Store teams use it to run operational programmes
(seasonal rollouts, compliance drives, refits), assign and track the activities those programmes
require, coordinate staff across departments, and pull performance reports by store and region.

The users are store staff, not office workers. That matters for two recurring design consequences:

- **Shift boundaries are real.** Work is handed over at fixed times, often in a hurry, often on a
  handheld. Bulk operations and partial success are normal, not edge cases.
- **Overdue work has consequences.** A missed compliance check is a regulatory exposure, not a late
  ticket. This is why SLA breach handling is an alerting concern with an escalation path rather than
  a status field.

## Modules

Five domain modules under `com.cognizant.storeops`. Each owns its own routes, service, repository,
and types. No module has a "utils" dependency on another module.

### `activities`
Operational activities — restocking runs, planogram resets, compliance checks, general store tasks.
The busiest module and the one most features touch.

- `Task` — id, title, description, status, priority, category, programmeId, assigneeId, storeId,
  dueDate, createdAt, updatedAt
- `TaskStatus` — `TODO` | `IN_PROGRESS` | `DONE` | `BLOCKED`
- `TaskPriority` — `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`
- `TaskCategory` — `RESTOCKING` | `PLANOGRAM` | `AUDIT` | `COMPLIANCE` | `GENERAL`
- `AuditEntry` — id, taskId, previousStatus, newStatus, actingUserId, note, occurredAt. One row per
  applied status transition, written by the `activities` service and never updated or deleted. It is
  the handover record: a `succeeded` entry in a bulk result implies exactly one of these, and a
  `failed` entry implies none.

The module's entity is `Task`, not `Activity` — the module is named for the retail domain and the
type keeps the name the codebase already uses. "Activity" in prose and in an acceptance criterion
means a `Task`. Do not rename either.

### `programmes`
Store programmes and their staff membership.

- `Project` — id, name, description, storeId, ownerId, status, createdAt
- `ProjectMember` — projectId, userId, role
- `ProjectRole` — `STORE_MANAGER` | `DEPARTMENT_LEAD` | `ASSOCIATE`

### `staff`
Registration, authentication, profile. **Read-only to every other module.** No other module may
create, update, or delete a `User`.

- `User` — id, email, name, role, storeId, regionId, active
- `UserProfile` — userId, displayName, department, phone
- `StaffRole` — `REGIONAL_MANAGER` | `STORE_MANAGER` | `DEPARTMENT_LEAD` | `ASSOCIATE`
- `AuthToken` — token, userId, expiresAt

### `alerts`
In-app alerts triggered by operational events. **Event-driven only** — nothing outside `alerts`
constructs a `Notification` directly.

- `Notification` — id, userId, alertType, channel, status, payload, createdAt
- `NotificationChannel` — `IN_APP` | `EMAIL`
- `NotificationStatus` — `PENDING` | `SENT` | `READ` | `FAILED`
- `AlertType` — `INVENTORY` | `SLA_BREACH` | `SHIFT_HANDOVER` | `ESCALATION`

### `reports`
Store and regional performance summaries. **Read-only across the whole system** — it aggregates from
`activities`, `programmes`, and `staff` and writes only to its own `Report` store.

- `Report` — id, reportType, scopeId, status, generatedAt, payload
- `ReportType` — `STORE_SUMMARY` | `REGIONAL_ROLLUP` | `DEPARTMENT_PERFORMANCE`
- `ReportStatus` — `PENDING` | `READY` | `FAILED`

### `shared`
`AppException` and its subclasses, `GlobalExceptionHandler`, `StoreOpsEventBus`, event records,
common value types. No domain logic, no module-specific types.

- `StoreOpsUser` — id, storeId, regionId, role. The authenticated principal, resolved from an
  `AuthToken` and injected with `@AuthenticationPrincipal`. A read-only projection of `staff.User`,
  and it lives in `shared` precisely so that every controller and service in the system does not
  import a `staff` type to know who is calling.

## Ownership matrix

Who may do what to whom. This table is the fast answer to "can I call that from here?"

| From ↓ / To → | activities | programmes | staff | alerts | reports |
|---|---|---|---|---|---|
| **activities** | own | read via service | read via service | event only | never |
| **programmes** | read via service | own | read via service | event only | never |
| **staff** | never | never | own | event only | never |
| **alerts** | read via service | read via service | read via service | own | never |
| **reports** | read via service | read via service | read via service | event only | own |

"read via service" means calling the target module's `*Service` for a query. Never its repository.
"event only" means `StoreOpsEventBus.publish(...)` and nothing else — **including no reads.** "never"
means no dependency at all, in either direction, including imports of DTOs.

The "including no reads" is the part that gets assumed away. `alerts` and `reports` are pure
consumers, so nothing outside them may depend on `alerts.service` or `reports.service` for any
reason. ARCH-2's ArchUnit rules enforce exactly this, and a read is what they fail on most often.

`staff` depends on nothing. `reports` is depended on by nothing. If a change makes either of those
false, stop and escalate — it means the feature is reshaping the domain, which is a developer
decision, not a harness one.

## Base API surface

Nine endpoints exist before any harness-generated feature:

| Method + path | Module |
|---|---|
| `GET /api/activities` | activities |
| `POST /api/activities` | activities |
| `GET /api/activities/{id}` | activities |
| `PATCH /api/activities/{id}` | activities |
| `DELETE /api/activities/{id}` | activities |
| `GET /api/programmes` | programmes |
| `POST /api/programmes` | programmes |
| `POST /api/programmes/{id}/members` | programmes |
| `GET /api/alerts` | alerts |

`staff` is auth-only. `reports` has no base endpoints — its surface arrives with a feature.

## The four failure modes this harness exists to prevent

From the client's prior experiment with unstructured AI-assisted development. Every gate and every
rule in this harness traces back to one of these. When in doubt about whether a rule matters, check
which of these four it stops.

1. **Direct repository imports across modules** — bypassed the service boundary and the event bus.
2. **Raw `Error` / `RuntimeException` throws in service methods** — bypassed the typed `AppException`
   hierarchy, so the API returned 500s with stack traces where it should have returned typed codes.
3. **Tests that asserted HTTP status codes but not business rules** — a green suite over broken
   behaviour, which is worse than a red one because it ends review.
4. **Missing event bus integration** — state changes written straight into sibling module
   repositories, so side effects were invisible and untestable.

## Storage

In-memory repositories, no database. This is a reference codebase. Do not add a persistence
dependency, a migration tool, or a connection pool. Repository interfaces are written as if a
database were behind them so the boundary is real, but the implementation is a `ConcurrentHashMap`.
