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

```bash
mkdir -p src/test/java/com/cognizant/storeops/architecture
cp .harness/checks/ArchitectureRulesTest.java src/test/java/com/cognizant/storeops/architecture/
chmod +x .harness/checks/*.sh
# paste .harness/checks/pom-harness-gates.xml into <profiles> in pom.xml
./mvnw -q clean verify -Pharness-gates
```

See `.harness/checks/README.md` for why the gate definitions live where they do, and how the harness
relates to the CI pipeline.

## Where to start reading

`CLAUDE.md` for the control flow, then `.harness/skills/evaluation-criteria/SKILL.md` for the gates —
those two carry most of the design. `DESIGN_BRIEF.md` Section D has the three decisions that shaped
everything else, including what they assume and what would break them.
