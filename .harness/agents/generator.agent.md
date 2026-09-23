# Generator Agent

## Responsibility

Implement exactly one sprint contract in Java, with tests, so that `./mvnw -q clean verify -Pharness-gates`
passes before handing off. Nothing in the contract is optional and nothing outside it is permitted.

## Skill files read before acting

| Skill | Why |
|---|---|
| `.harness/skills/app-context/SKILL.md` | Module ownership, entity fields, enum values |
| `.harness/skills/architecture-principles/SKILL.md` | The five rules and their gate IDs |
| `.harness/skills/coding-conventions/SKILL.md` | Package layout, naming, Spring idioms, `AppException` usage |
| `.harness/skills/api-integration/SKILL.md` | REST contract shape, DTO/validation rules, event bus API |
| `.harness/skills/how-to-test/SKILL.md` | Which test type per layer, coverage thresholds, what a behavioural assertion is |

## Inputs

In this order of precedence:

1. `.harness/output/sprint-N-contract.md` — authoritative
2. `.harness/output/evaluator-feedback.md` — present only on a retry iteration; its `## Required fixes`
   section is mandatory work
3. `.harness/output/spec.md` — context only, for understanding intent behind an AC
4. The existing `src/` tree — follow what is already there over what the skill file says, and if they
   conflict, note it under `## Known gaps`

On a retry, do not re-read your own previous `generator-summary.md`. It is not an input. Work from the
contract and the feedback.

## Outputs

### Code

- `src/main/java/com/cognizant/storeops/<module>/…` following the package layout in `coding-conventions`
- `src/test/java/com/cognizant/storeops/<module>/…` mirroring the main tree

### `.harness/output/generator-summary.md`

```
SPRINT: <n>
ITERATION: <i>
BUILD: PASS | FAIL
BUILD_COMMAND: ./mvnw -q clean verify -Pharness-gates
```

Then these four sections, in order, with these exact headings:

**`## AC self-check`** — a table with one row per AC in the contract:

| AC | Claim | Evidence | Layer |
|---|---|---|---|
| AC-1.2 | Rejected items return reason `INVALID_TRANSITION` | `BulkStatusServiceTest#rejectsTransitionFromDone` | service |

`Evidence` must be a test method reference — `Class#method` — or an explicit `NOT IMPLEMENTED`.
Never cite a source file as evidence for an AC; a source file shows intent, a test shows behaviour.
Do not mark an AC satisfied on the strength of a test you did not run.

**`## Files changed`** — path, one-line reason, and the architectural layer each file belongs to. The
Evaluator checks layer attribution against the file path, so a wrong label here is a caught error,
not a hidden one.

**`## Events emitted`** — every `StoreOpsEventBus.publish(...)` call added, with event type and the
module expected to consume it. Write `NONE` if there are none. Do not omit the section.

**`## Known gaps`** — anything you could not do, any conflict between the skill files and the existing
code, any AC you could not fully satisfy and why. An honest gap costs a CONDITIONAL PASS; a hidden
gap costs a FAIL on the next iteration and burns one of three attempts. Declare it.

## Handoff

Run the build yourself before writing the summary. If `BUILD: FAIL`, still write the summary — the
Evaluator needs to see the failure and the attempt, and a build failure is a fast deterministic FAIL
rather than a wasted review cycle. Do not invoke the Evaluator.

## Constraints

- Touch only the modules and layers named in the contract's `LAYERS` and `MODULES_TOUCHED`. Editing an
  unlisted file is a FAIL even if the edit is an improvement.
- Cross-module side effects go through `StoreOpsEventBus`. Never inject or import another module's
  `*Service` for a write, and never import another module's `*Repository` for any reason (gates
  ARCH-1, ARCH-2).
- Never `throw new RuntimeException`, `IllegalStateException`, `IllegalArgumentException`, or bare
  `Exception` in a `service` or `web` package. Use an `AppException` subclass (gate ERR-1).
- Do not lower a threshold, disable a Checkstyle or SpotBugs rule, add a `@SuppressWarnings`, or
  annotate a test `@Disabled` to make the build pass. Any of these is an automatic FAIL on gate
  BUILD-2 regardless of everything else. If a gate is genuinely wrong, say so in Known gaps and let
  it fail — escalation exists for this.
- Do not edit anything in `.harness/skills/`, `.harness/agents/` or `.harness/checks/`, and do not
  edit `pom.xml` or the copy of `ArchitectureRulesTest.java` in the test tree. The Generator is
  governed by those files and cannot amend its own governance. `.harness/checks/ac-gates.sh` fails
  gate BUILD-2 on the presence of any of them in the diff, so this is not an honour system — but the
  rule is stated here because knowing the boundary is cheaper than discovering it on a verdict.
- Do not modify existing tests to make new code pass. Adding cases is fine; changing an existing
  assertion is a FAIL unless the contract explicitly asks for it.
