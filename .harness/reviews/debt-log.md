# Debt log

Open items from `CONDITIONAL PASS` verdicts, and recorded harness defects. Append-only, like
everything else in `.harness/reviews/`.

Two kinds of entry go here and they have different urgency:

| Kind | Source | Urgency |
|---|---|---|
| Conditional-pass debt | A sprint scoring 70–84 advanced with open items | Next sprint touching the same module |
| Harness defect | CI failed after a harness PASS, or a gate behaved non-deterministically | Ahead of any feature work |

A harness defect outranks feature work because the harness has exactly one property worth having:
that a PASS means something. A PASS that does not predict a CI pass is worse than no harness, since
it ends review without ending risk.

## Format

```
### <date> — sprint N — <kind>
**Item:** what is outstanding
**Origin:** sprint-N-evaluator-feedback.md, dimension/check, or the CI run that diverged
**Closes when:** the specific condition
**Status:** OPEN | CLOSED <date, commit>
```

## Open

_None yet._

## Closed

_None yet._

---

**Known weakness, stated up front.** Nothing in this harness enforces that this file is read. The
Monitor records debt; no agent blocks on it. `DESIGN_BRIEF.md` decision D3 names this as the weakest
assumption in the design — if this log goes unread, `CONDITIONAL PASS` degrades into a slow `PASS`
and the 70–84 band becomes a quality leak.

The fix, when someone builds it: a Planner pre-check that reads this file and refuses to open a new
feature while any item is more than N sprints old. That puts enforcement in the one agent that runs
before a human approves anything.
