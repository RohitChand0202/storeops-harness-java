# Demonstration run prompt

The exact prompt used to invoke the harness for the demonstration run.

```
@planner Add shift handover bulk update — PATCH /api/activities/bulk-status so outgoing shift
staff can mark multiple operational activities DONE or BLOCKED in one request, with partial
failure handling and an audit entry per updated activity.
```

Entered in Claude Code from the repository root, with the baseline commit
`baseline: generated StoreOps scaffold` checked out.

After the Planner writes `.harness/output/spec.md` and the three sprint contracts, the only further
developer input is the single word:

```
APPROVED
```

From that point the Generator/Evaluator loop runs to completion or escalation with no manual
intervention, and the per-sprint artefacts it produces are archived to `.harness/reviews/`.

## Feature source

Section 3.4 of the case study, option 1 — shift handover bulk update. Chosen over the other three
because it combines partial-failure semantics (the hardest thing to specify in a testable AC) with
a cross-module event emission (`activities` → `alerts`), so the demonstration exercises both the
LLM-assessed and the automated halves of the evaluation framework.
