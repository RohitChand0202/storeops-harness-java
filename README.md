# storeops-harness-java

A governed agentic development harness for the StoreOps API, built with Claude Code.

Capstone Case Study 1 — Build Track, AI-Native Tech Architect Programme.
Stack: Java 17, Spring Boot 3.x, Maven, JUnit 5 + MockMvc, Checkstyle, SpotBugs, JaCoCo, ArchUnit.

## What this is

StoreOps is a retail store operations REST API. This repository holds StoreOps *and* the harness that
governs further development of it: four agents with file-only handoffs, eight skill files encoding
StoreOps-specific architectural rules, and an eleven-gate evaluation framework that converts variable
LLM output into a deterministic accept/reject verdict.

It exists because AI coding tools without architectural guardrails drift. The client's prior
experiment produced four specific failure modes — cross-module repository imports, raw exception
throws, tests asserting status codes without business rules, and missing event bus integration — and
every gate in this harness traces to one of them.

## Running it

```
@planner <one-sentence feature description>
```

Review `.harness/output/spec.md`, then:

```
APPROVED
```

The Generator/Evaluator loop then runs to completion or escalation with no further input. Routing,
iteration bounds, and escalation are defined in `CLAUDE.md`.

## Layout

| Path | What |
|---|---|
| `CLAUDE.md` | Orchestrator — entry format, routing, iteration bounds, context scoping, CI relationship |
| `DESIGN_BRIEF.md` | Architectural intent: intent decomposition, governance, non-determinism strategy, decision log |
| `PROMPT.md` | The feature prompt used for the demonstration run |
| `DEPLOYMENT.md` | Docker and cloud deployment, with the verification calls |
| `REFLECTION.md` | Post-run assessment |
| `JOURNAL.md` | Design decisions and reversals captured during the build |
| `.harness/agents/` | Planner, Generator, Evaluator, Monitor definitions |
| `.harness/skills/` | Eight skill files — feedforward context read before an agent acts |
| `.harness/checks/` | ArchUnit rules, Checkstyle config, Maven gate profile, the `AC-1`/`AC-2`/`BUILD-2` diff gate |
| `.harness/templates/` | Handoff artefact formats |
| `.harness/output/` | Working files during a run — gitignored |
| `.harness/reviews/` | Archived sprint artefacts — append-only governance audit trail |
| `src/`, `tests/` | StoreOps source and tests |

## Setup

The scaffold and the gate wiring are already in place. Needs a JDK 17+ and Maven:

```bash
mvn clean verify -Pharness-gates
```

Measured on the committed baseline: Checkstyle 0 violations, SpotBugs clean, 95 tests green, 9
ArchUnit rules passing, and line coverage of 98.4% service / 100% web / 100% shared / 99.1% overall
against thresholds of 80 / 70 / 60 / 70.

Run the app and drive it:

```bash
mvn spring-boot:run
curl -s localhost:8080/api/activities
```

Test the gates themselves — ten assertions, each a gate accepting what it should accept or rejecting
what it should reject:

```bash
.harness/checks/fixtures/gate-selftest.sh
```

See `.harness/checks/README.md` for why the gate definitions live where they do, and how the harness
relates to the CI pipeline.

## What the baseline deliberately does not do

The scaffold is stub implementations, per the case study: enough structure to compile, run, pass
every gate and be extended by a harness run. Three gaps are intentional and are the obvious places
for a feature sprint to start:

- **Auth is not wired.** `StaffService.resolveToken` exists and is tested, but no filter or argument
  resolver calls it, so no endpoint enforces a `StaffRole` or a store scope. The scoping rules those
  endpoints will need are already specified in `api-integration`. Consequently the two list
  endpoints that the case study describes as "for the authenticated store" and "for the authenticated
  user" take `storeId` and `userId` as request parameters instead.
- **No programme-close endpoint.** `ProgrammeService.close` publishes `ProgrammeClosedEvent` and is
  tested, but §3.6's nine base endpoints do not include a route to it, so it is reachable only from
  code.
- **No SLA sweep.** `SlaBreachedEvent` and its `alerts` listener exist and are tested; nothing
  schedules the check that would raise one.

The event bus is wired and demonstrable end to end without any of the above: `PATCH
/api/activities/{id}` to `BLOCKED` publishes `TaskBlockedEvent`, the `alerts` listener consumes it,
and `GET /api/alerts?userId=...` returns the resulting `ESCALATION` notification — with no reference
to `alerts` anywhere under `activities/`.

## Where to start reading

`CLAUDE.md` for the control flow, then `.harness/skills/evaluation-criteria/SKILL.md` for the gates —
those two carry most of the design. `DESIGN_BRIEF.md` Section D has the three decisions that shaped
everything else, including what they assume and what would break them.
