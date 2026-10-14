# StoreOps API and governed development harness

StoreOps is a Java 17, Spring Boot REST API for retail operations. The repository also contains a four-agent development harness with Planner, Generator, Evaluator, and Monitor contracts. `CLAUDE.md` describes the intended Claude Code orchestration; `.harness/` contains the contracts, skills, and executable checks. `DESIGN_BRIEF.md` explains the architecture and gate choices.

## Build and run

Install JDK 17 or newer. The committed Maven wrapper downloads the pinned Maven 3.9.16 distribution on first use.

```bash
./mvnw clean verify -Pharness-gates
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` in place of `./mvnw`. The gate profile runs the tests, Checkstyle, SpotBugs, JaCoCo coverage checks, and ArchUnit rules. CI runs the same `clean verify -Pharness-gates` command. The wrapper distribution has a SHA-256 checksum in `.mvn/wrapper/maven-wrapper.properties`.

All `/api/**` calls require `Authorization: Bearer <token>`. There are no built-in credentials. For a local demo, set the `STOREOPS_BOOTSTRAP_*` environment variables described in [DEPLOYMENT.md](DEPLOYMENT.md), then start the app. `/actuator/health` remains available without a token. Data is held in memory and is lost when the process exits.

## Feature demonstration

The governed feature is `PATCH /api/activities/bulk-status`: an outgoing shift can submit up to 200 status changes. Each item is evaluated independently; successful transitions create one audit entry each, and a successful batch publishes one `ShiftHandoverCompletedEvent`. The alerts module consumes that event through the shared event bus. The HTTP response is `200` when all items succeed and `207` when any item fails. See [DEPLOYMENT.md](DEPLOYMENT.md) for a runnable request sequence.

The spec and three sprint contracts from this assisted run are archived in `.harness/reviews/`; `.harness/output/` is an ignored working directory. The spec records `AWAITING APPROVAL` because no separate human `APPROVED` message was received for the Claude Code workflow. The user's instruction to implement missing work authorized the code changes in this coordinated Codex-assisted session. See `PROMPT.md` and the integration contract for the run's actual provenance.

## Repository map

| Path | Purpose |
|---|---|
| `src/main/java` | Activities, programmes, staff, alerts, reports, and shared API code |
| `src/test/java` | Unit, web, integration, and architecture tests |
| `.harness/agents/` | Planner, Generator, Evaluator, and Monitor definitions |
| `.harness/skills/` | StoreOps architecture and evaluation context |
| `.harness/checks/` | Gate rules and self-tests |
| `.harness/reviews/` | Archived evidence from completed review cycles |
| `Dockerfile`, `compose.yaml` | Local container deployment |
| `.github/workflows/ci.yml` | Repeatable GitHub Actions build |

Run the gate self-tests with Git Bash on Windows or Bash elsewhere:

```bash
bash .harness/checks/fixtures/gate-selftest.sh
bash .harness/checks/fixtures/ac-gates-selftest.sh
```

## Current limits

- There is no shift roster. The handover alert listener selects an eligible active department lead in the same store other than the outgoing actor; it cannot identify the actual incoming shift lead.
- The in-memory repositories do not persist data across restarts and do not support multiple independent app instances sharing state.
- Regional cross-store access is not enabled because there is no authoritative store-to-region mapping.
- An SLA sweep is not scheduled, although `SlaBreachedEvent` and its alert listener exist.

Start with `CLAUDE.md` for the harness flow and `.harness/skills/evaluation-criteria/SKILL.md` for the gate definitions. `DESIGN_BRIEF.md` contains the architectural decisions.
