# StoreOps Harness — Orchestrator

This file is the control plane for AI-assisted development on StoreOps. Claude Code reads it
automatically on launch. Do not add feature code or business rules here — this file routes work,
it does not do work.

Stack: Java 17, Spring Boot 3.x, Maven, JUnit 5 + MockMvc, Checkstyle + SpotBugs, JaCoCo, ArchUnit.

---

## 1. Entry point

A developer starts a run with a single line:

```
@planner <feature description in one sentence>
```

Example, and the one used for the demonstration run in `PROMPT.md`:

```
@planner Add shift handover bulk update — PATCH /api/activities/bulk-status so outgoing shift
staff can mark multiple activities DONE or BLOCKED in one request, with partial failure handling
and an audit entry per updated activity.
```

Nothing else is required from the developer until the approval gate below. Do not ask the developer
to confirm intermediate steps, pick file names, or approve individual iterations.

## 2. Agents

| Agent | File | Invoked when |
|---|---|---|
| Planner | `.harness/agents/planner.agent.md` | Developer sends `@planner <feature>` |
| Generator | `.harness/agents/generator.agent.md` | A sprint contract is active and not yet PASS |
| Evaluator | `.harness/agents/evaluator.agent.md` | Generator has written `generator-summary.md` |
| Monitor | `.harness/agents/monitor.agent.md` | A sprint reaches terminal state (PASS, CONDITIONAL PASS, or ESCALATED) |

Each agent runs in its own invocation. Read the agent file, then read only the skill files that
agent declares. Do not load skill files belonging to another agent — see §6.

## 3. Sequence

```
@planner <feature>
   → Planner writes .harness/output/spec.md            (STATUS: AWAITING APPROVAL)
   → Planner writes .harness/output/sprint-N-contract.md for each sprint
   → HALT. Print the spec path and wait.

developer types APPROVED
   → for each sprint, in order:
        Generator  → src/, tests/, .harness/output/generator-summary.md
        Evaluator  → .harness/output/evaluator-feedback.md   (VERDICT: …)
        route on verdict (§4)
        Monitor    → .harness/reviews/sprint-N-run-log.md
   → when the last sprint is terminal, print the run summary and stop.
```

The approval gate is the only hard stop in a healthy run. `APPROVED` must appear as a standalone
developer message. Any other reply to the spec (edits, questions, "change sprint 2") re-invokes the
Planner with that feedback and produces a new `spec.md`; the approval gate then repeats.

## 4. Routing logic

Read the first line of `.harness/output/evaluator-feedback.md`. It is machine-readable by contract:

```
VERDICT: PASS | CONDITIONAL PASS | FAIL
SPRINT: <n>
ITERATION: <i>
HARD_GATES_FAILED: <comma-separated gate IDs, or NONE>
SCORE: <0-100>
```

| Verdict | Action |
|---|---|
| `PASS` | Archive artefacts (§5), run Monitor, advance to sprint N+1. If no sprint remains, end the run. |
| `CONDITIONAL PASS` | Same as PASS, and append the open items from the feedback file to `.harness/reviews/debt-log.md`. Do not retry. |
| `FAIL`, iteration < 3 | Re-invoke the Generator for the same sprint with `evaluator-feedback.md` as its primary input. Increment ITERATION. |
| `FAIL`, iteration = 3 | Stop the loop. Write `.harness/output/escalation.md` (§4.1). Run Monitor with `escalation: true`. Do not advance to the next sprint. |

If the verdict line is missing or unparseable, treat it as `FAIL` and count the iteration. A verdict
the orchestrator cannot read is a broken handoff, not a pass — the harness fails closed.

### 4.1 Escalation output

`.harness/output/escalation.md` must contain, and nothing beyond:

- `SPRINT`, `ITERATION_COUNT: 3`, `FEATURE`, timestamp
- The blocking issue: which hard gate IDs failed on all three iterations, with the exact tool output line
- What changed between iterations (one line per iteration) — this distinguishes "Generator is looping
  on the same mistake" from "Generator is fixing one thing and breaking another"
- Files touched across all three iterations
- `RECOMMENDED_ACTION`: one of `REVISE_CONTRACT`, `REVISE_SKILL_FILE`, `HUMAN_IMPLEMENT`, with a reason
- `git stash` reference or branch name holding the last failed attempt

Recipient: the developer who invoked the run. Copy to the engineering standards team when
`HARD_GATES_FAILED` includes `ARCH-1`, `ARCH-2`, or `ERR-1`, because those three are the gates that
exist for the standards team's own stated concerns and a repeated failure is evidence their rule is
either unclear in the skill files or genuinely too strict.

## 5. Artefact archival

Working files live in `.harness/output/` and are gitignored during a run. On each terminal sprint
verdict, copy to `.harness/reviews/` with the sprint prefix and commit:

- `sprint-N-contract.md`
- `sprint-N-generator-summary.md`
- `sprint-N-evaluator-feedback.md`
- `sprint-N-run-log.md`

`spec.md` is copied once per run. `.harness/reviews/` is append-only: never rewrite an archived
verdict, even a wrong one. A superseded verdict gets a new file, not an edit. That property is what
makes the directory usable as an audit trail.

## 6. Context scoping

Long runs degrade because each agent inherits the previous agent's reasoning. This harness prevents
that structurally: **agents communicate only through files, never through conversation history.**

- Invoke each agent as a fresh sub-agent task. It receives: its own agent file, its declared skill
  files, and its declared input artefacts. Nothing else.
- The Generator does not see the Evaluator's reasoning, only `evaluator-feedback.md`. The Evaluator
  does not see the Generator's reasoning, only `generator-summary.md` and the diff.
- Reset context at every sprint boundary. Carry forward only: `spec.md`, the next sprint contract,
  and the list of files the previous sprint touched.
- On a FAIL retry, the Generator gets the contract plus the feedback file — not the previous
  iteration's summary. Stale self-justification is the main cause of a Generator repeating a mistake.

Token budget per agent invocation, and the reason the skill files are sized the way they are:

| Agent | Skill files read | Approx. budget |
|---|---|---|
| Planner | app-context, architecture-principles, sprint-decomposition | ~6k |
| Generator | app-context, architecture-principles, coding-conventions, api-integration, how-to-test | ~14k |
| Evaluator | architecture-principles, how-to-review, evaluation-criteria | ~10k |
| Monitor | app-context | ~2k |

The Generator is the expensive agent and the one whose output the gates actually measure, so it gets
the deepest skill files. The Monitor reads almost nothing by design — it summarises artefacts that
are already structured, and giving it architectural context would only invite it to editorialise.

## 7. Relationship to CI

The harness **precedes and duplicates** CI. It does not replace it.

- Evaluator automated gates invoke the same Maven lifecycle CI invokes: `./mvnw -q clean verify`
  with the `harness-gates` profile, which activates Checkstyle, SpotBugs, JaCoCo `check`, and the
  ArchUnit test class. Same commands, same thresholds, same config files.
- One gate does not and cannot run in CI: `.harness/checks/ac-gates.sh` decides `AC-2` and the
  countable half of `AC-1` by parsing a sprint contract, and CI has no sprint contract — a merge to
  `main` is not a sprint. That is the boundary between the two systems stated precisely. CI enforces
  what is true of the codebase; the harness enforces what was true of the contract the code was
  written against, and only the harness has that contract in hand.
- CI stays the enforcing authority on `main`. A harness PASS is a prediction that CI will pass, not
  a substitute for it — the harness runs on a developer machine and can be skipped or run stale.
- Harness configuration lives in `.harness/`. Pipeline configuration lives in `.github/workflows/`.
  They share the Maven profile and never duplicate thresholds: if coverage moves, it moves in
  `pom.xml`, and both read it from there. See `.harness/checks/README.md`.

If CI fails after a harness PASS, that is a harness defect. Record it in `.harness/reviews/debt-log.md`
and treat the divergence as the highest-priority harness fix, ahead of any feature work.
