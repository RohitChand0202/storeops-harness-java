# Demonstration feature statement

This is the source feature statement used to draft the shift handover acceptance contracts for the assisted implementation. The `@planner` prefix shows the intended entry format of the Claude Code harness described in `CLAUDE.md`:

```text
@planner Add shift handover bulk update - PATCH /api/activities/bulk-status so outgoing shift
staff can mark multiple operational activities DONE or BLOCKED in one request, with partial
failure handling and an audit entry per updated activity.
```

The implementation in this repository was carried out in one coordinated Codex-assisted session from the baseline commit `bc39197` (`baseline: generated StoreOps scaffold`). This text was not entered as a standalone Claude Code invocation. No standalone `APPROVED` message was given for the Claude Code approval gate, and the archived spec therefore remains marked `AWAITING APPROVAL`. The three sprint contracts describe planned risk boundaries; they do not assert three sequential Generator/Evaluator cycles. The combined integration contract and review artefacts describe what was actually checked.

## Feature source

Section 3.4 of the case study, option 1 - shift handover bulk update. This scenario combines partial-failure semantics with cross-module event emission from `activities` to `alerts`, making it useful for checking both acceptance evidence and architecture gates.
