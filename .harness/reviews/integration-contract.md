SPRINT: 1-3 (combined integration review)
TITLE: Shift handover bulk status update
DEPENDS_ON: sprint-1-contract.md, sprint-2-contract.md, sprint-3-contract.md
MODULES_TOUCHED: activities, alerts, staff, shared
LAYERS: domain, repository, service, web, events, tests

## Review basis

This contract joins the acceptance criteria from
`.harness/reviews/sprint-1-contract.md`, `.harness/reviews/sprint-2-contract.md`, and
`.harness/reviews/sprint-3-contract.md` for one integration review.
Those three files retain the authoritative GIVEN/WHEN/THEN wording and scope boundaries.
The implementation was produced in one coordinated Codex-assisted session; this document does not
represent three sequential passing commits, separate Generator iterations, or a standalone
Claude Code Evaluator approval.

## Acceptance criteria in scope

| ID | Source | Observable to verify |
| --- | --- | --- |
| AC-1.1 | Sprint 1 | Three eligible transitions succeed in request order with prior/new statuses and persisted targets. |
| AC-1.2 | Sprint 1 | One audit entry captures the transition, actor, note, and operation time. |
| AC-1.3 | Sprint 1 | A DONE activity rejects BLOCKED with INVALID_TRANSITION and no state or audit change. |
| AC-1.4 | Sprint 1 | A repeated id succeeds once, then fails ALREADY_IN_STATUS, with one audit entry. |
| AC-1.5 | Sprint 1 | A valid id succeeds and persists while an unknown id fails NOT_FOUND without audit. |
| AC-1.6 | Sprint 1 | A local id succeeds while a foreign-store id fails FORBIDDEN without changing the foreign task. |
| AC-1.7 | Sprint 1 | TODO and IN_PROGRESS targets fail UNSUPPORTED_TARGET_STATUS without writes. |
| AC-1.8 | Sprint 1 | All-foreign ids cause a whole-request ForbiddenException with no writes. |
| AC-1.9 | Sprint 1 | A missing actor causes a whole-request UnauthorizedException with no writes. |
| AC-1.10 | Sprint 1 | An all-rejected request returns ordered item reasons with no task save or audit append. |
| AC-2.1 | Sprint 2 | All-success PATCH returns 200 and complete count and success fields. |
| AC-2.2 | Sprint 2 | Mixed PATCH returns 207 and complete success/failure counts and item fields. |
| AC-2.3 | Sprint 2 | All-failed PATCH returns 207 and each failed id and reason. |
| AC-2.4 | Sprint 2 | Each of five invalid body forms returns 400 VALIDATION_FAILED without calling the service. |
| AC-2.5 | Sprint 2 | Missing or invalid bearer returns 401 UNAUTHORIZED without calling the service. |
| AC-2.6 | Sprint 2 | The resolved token actor, rather than a body actor, reaches the service. |
| AC-3.1 | Sprint 3 | One event contains actor, store, ordered successful ids only, and operation time. |
| AC-3.2 | Sprint 3 | An all-rejected operation publishes no event and appends no audit. |
| AC-3.3 | Sprint 3 | Read-only staff lookup returns only active leads in the requested store. |
| AC-3.4 | Sprint 3 | An event creates one persisted SHIFT_HANDOVER IN_APP alert for an eligible lead with store and count. |
| AC-3.5 | Sprint 3 | No eligible lead creates no alert and leaves successful activity writes intact. |
| AC-3.6 | Sprint 3 | Alert-creation failure does not escape or reverse successful activity writes. |

## Co-delivered baseline repairs and integration boundary

The same assisted session also changed existing activity routes to use the authenticated principal
for store access and audit attribution, and repaired the single-activity TODO-to-IN_PROGRESS
transition. These are explicit integration-review scope additions beyond the three original feature
sprint contracts. The original contracts said single-activity PATCH behavior was out of scope, so
the review must disclose this semantic expansion rather than claim literal per-sprint scope
compliance. Tests for these repairs are part of the combined tree.

No database, shift roster, new dependency, bulk delete, per-activity bulk alert, or direct write
from activities to alerts is added. The combined review checks the completed code and tests as one
working tree; it does not infer intermediate sprint build results from the final state.
