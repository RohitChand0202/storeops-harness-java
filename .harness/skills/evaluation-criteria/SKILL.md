# evaluation-criteria

**Purpose.** The scoring framework. Four dimensions summing to 100%, eleven hard gates, fixed
arithmetic, and three verdict thresholds. Read by the Evaluator only.

The design goal: **given the same Generator output, this framework produces the same verdict every
time.** Everything below exists to remove a place where an LLM assessor could reasonably decide
either way.

---

## Dimensions

| # | Dimension | Weight | Why this weight for StoreOps |
|---|---|---|---|
| 1 | Architectural compliance | 40% | Three of the client's four failure modes are architectural. This is the dimension the standards team is withholding approval over, and it is the one a human reviewer is worst at catching by eye. |
| 2 | Acceptance criteria satisfaction | 25% | The feature has to work. Weighted below architecture deliberately: a correct feature that violates a module boundary is worse than an incomplete feature that respects it, because the boundary violation ships silently and the missing capability does not. |
| 3 | Test quality and coverage | 20% | Failure mode 3 — green suites over unverified business rules. Separated from dimension 2 because an AC can be satisfied by code whose test proves nothing. |
| 4 | Build integrity | 15% | Cheapest, most deterministic, and the pre-condition for trusting anything else. Low weight because it is nearly binary in practice; its power is in its hard gates, not its score. |

**Total: 100%.**

---

## Hard gates

A hard gate failure produces `FAIL` immediately, regardless of every other score. No dimension
scoring runs. Each is deterministic: same code, same tool, same answer.

### Dimension 1 — Architectural compliance

| Gate | Condition | Proof |
|---|---|---|
| `ARCH-1` | Zero cross-module repository references | `ArchitectureRulesTest#repositoriesAreModulePrivate` passes |
| `ARCH-2` | Zero dependencies from any module on `alerts.service` or `reports.service` | `ArchitectureRulesTest#alertsIsReachedByEventOnly` passes |
| `ERR-1` | Zero raw `RuntimeException`/`IllegalStateException`/`IllegalArgumentException` constructions in `service` or `web` | Checkstyle `RegexpSinglelineJava` **and** `ArchitectureRulesTest#servicesThrowOnlyAppExceptions` both pass |
| `ARCH-3` | Layer dependency direction holds | `ArchitectureRulesTest#layersAreRespected` passes |
| `ARCH-4` | `reports` has no dependency on another module's repository | `ArchitectureRulesTest#reportsNeverWrite` passes |

### Dimension 2 — Acceptance criteria satisfaction

| Gate | Condition | Proof |
|---|---|---|
| `AC-1` | Every AC in the contract has a cited test method that exists and asserts on the AC's observables | Two parts. `.harness/checks/ac-gates.sh` proves the citation resolves — it fails on a missing AC row, a `NOT IMPLEMENTED`, a cited source file, or a method that does not exist. Then the Evaluator opens each resolved test and confirms it asserts the AC's observables. |
| `AC-2` | No file in the diff falls outside the contract's `MODULES_TOUCHED` × `LAYERS` | `.harness/checks/ac-gates.sh` exits 0. Parses both fields from the contract header and maps every changed `src/` path to a module and layer. |

`AC-2` is decided entirely by exit code, and `AC-1`'s countable half with it. That is deliberate:
dimension 2 previously had no automated gate, which made the two gates protecting the link between
verdict and contract the only ones with nothing but an LLM assessor behind them. The half of `AC-1`
that cannot be automated — whether an assertion actually maps onto the observable the THEN names —
is still read, and `how-to-review` step 3 is the procedure for it.

### Dimension 3 — Test quality and coverage

| Gate | Condition | Proof |
|---|---|---|
| `TEST-1` | Coverage thresholds met: service 80%, web 70%, shared 60%, overall 70% | `mvn jacoco:check` under `-Pharness-gates` exits 0 |
| `TEST-2` | Every business-rule AC has at least one assertion on state or outcome, not status alone | Evaluator reads each cited test. Status-only assertion fails the gate. |

### Dimension 4 — Build integrity

| Gate | Condition | Proof |
|---|---|---|
| `BUILD-1` | Compile, Checkstyle, SpotBugs, and all tests pass | `./mvnw -q clean verify -Pharness-gates` exits 0 |
| `BUILD-2` | No new suppression of any check | Three parts, all automated: Checkstyle `BUILD-2-suppression` on main sources; Checkstyle `BUILD-2-disabledTest` / `BUILD-2-commentedArchRule` on test sources; and `.harness/checks/ac-gates.sh`, which fails the gate if `pom.xml`, `.harness/checks/`, `.harness/skills/`, `.harness/agents/` or `ArchitectureRulesTest.java` appears in the diff |

**Eight of the eleven gates are decided entirely by a tool exit code** — `ARCH-1`..`ARCH-4`,
`ERR-1`, `TEST-1`, `BUILD-1`, `AC-2`. Two are part-automated: `AC-1`, where a script resolves the
citation and the Evaluator reads the assertions, and `BUILD-2`, where the tools catch every
suppression *shape* but only the Evaluator can judge whether a `pom.xml` edit the contract asked for
is legitimate. One — `TEST-2` — is decided by reading a cited test.

So ten of eleven gates cannot be reached by an LLM judgement without a tool having spoken first, and
the two that need reading are the two where an assessor could drift. That is why `how-to-review`
specifies the procedure for them in detail rather than leaving it to judgement.

The `pom.xml` case is worth knowing in detail, because it is the reason `BUILD-2` is not purely
mechanical: lowering a JaCoCo threshold produces a *passing* build by design, so no linter can see
it. `ac-gates.sh` closes the common case by failing on the file's mere presence in the diff. What
stays with the Evaluator is the narrow judgement of whether a contract legitimately asked for that
edit — and the answer is almost always no.

### Gate commands

```bash
# BUILD-1, ERR-1 (Checkstyle half), BUILD-2 (both Checkstyle halves), TEST-1, all ArchUnit gates
./mvnw -q clean verify -Pharness-gates

# AC-1 (citation half), AC-2, BUILD-2 (diff-scan half). Exit 0 or the gate failed.
.harness/checks/ac-gates.sh \
    --contract .harness/output/sprint-N-contract.md \
    --summary  .harness/output/generator-summary.md \
    --base     <baseline or last passing sprint commit>

# ARCH-1..4 and ERR-1 (ArchUnit half) in isolation, for a faster retry loop
./mvnw -q test -Dtest=ArchitectureRulesTest

# TEST-1 in isolation
./mvnw -q jacoco:check

# BUILD-2, both Checkstyle halves, without running tests
./mvnw -q checkstyle:check@harness-checkstyle-main \
        checkstyle:check@harness-checkstyle-build2-tests
```

`ac-gates.sh` ends with a fixed three-line block — `AC-1:`, `AC-2:`, `BUILD-2:`, each `PASS`, `FAIL`
or `SKIPPED`. Copy those lines into `## Gate results` as they are. A check the script cannot decide
prints `UNDETERMINED:` and exits 1, which is the fail-closed rule below implemented in a tool rather
than left to the assessor's discipline.

Record the exact failing line of tool output in `## Gate results`. Quote it; do not paraphrase.

### Why none of these can be a soft check

| Gate | If it were advisory |
|---|---|
| `ARCH-1` | The violation compiles, passes tests, and behaves correctly today. No test catches it. Advisory means it ships — this is failure mode 1 verbatim. |
| `ARCH-2` | A direct service call is the shorter code and the statistically likely generation. Advisory means it is written every sprint (failure mode 4). |
| `ERR-1` | Produces a 500 where the contract promises a typed code. The Generator writing the throw also writes the test's expected status, so the suite agrees with the bug (failure mode 2). |
| `ARCH-3` | Logic duplicates between controller and service, then drifts, and the same activity behaves differently per route. |
| `ARCH-4` | A report run mutates the data the next run reads. Two identical requests return different numbers and the audit trail is already gone. |
| `AC-1` | The loop terminates on a feature that was not built. This gate is the only thing connecting the verdict to the contract. |
| `AC-2` | Scope creep accumulates invisibly across sprints and each sprint's diff looks locally reasonable. |
| `TEST-1` | Coverage drifts down monotonically. No single sprint ever looks like the one that broke it. |
| `TEST-2` | Failure mode 3 exactly: a green suite over unverified rules, which is worse than a red one because it ends review. |
| `BUILD-1` | Everything downstream is unreliable. A verdict on code that does not compile is meaningless. |
| `BUILD-2` | Every other gate becomes optional. This is the gate that protects the gates. |

---

## Scoring

Runs **only** when all eleven hard gates pass.

Each dimension has a checklist of binary checks, equally weighted within the dimension:

```
dimension_raw   = (checks_passed / checks_total) × 100     # integer, truncated
contribution    = dimension_raw × dimension_weight
total           = sum of contributions, rounded half-up to integer
```

Integer arithmetic with truncation at the dimension level and one rounding at the end. Fixed order of
operations, so the same inputs cannot produce a different total.

### Dimension 1 checklist (40%)

1. All ArchUnit rules pass (already gated; scored so the dimension is never zero-weighted)
2. Cross-module reads go through a service interface, not an impl
3. Events published from the service layer, after the state change, once per operation
4. Listeners reside in the consuming module
5. `StoreOpsEventBus` injected rather than `ApplicationEventPublisher` directly
6. No business rule in a `web` class
7. No `catch` block that absorbs a failure and reports success

### Dimension 2 checklist (25%)

1. Every AC satisfied by a cited, verified test
2. Every deliverable in the contract present in the diff
3. Nothing from the contract's `Out of scope` section present
4. `Files changed` layer labels match actual packages
5. `Events emitted` section matches the `publish` calls in the diff
6. `Known gaps` is either empty or accurate — nothing found in review that should have been declared

Check 6 is what makes declaring a gap cheaper than hiding one. A declared gap costs a point here; a
gap the Evaluator finds costs this point *and* the AC.

### Dimension 3 checklist (20%)

1. Coverage thresholds met per package (gated; scored)
2. Every business-rule AC has a state or outcome assertion
3. At least one negative assertion per business rule (`never()`, unchanged-state, empty-collection)
4. No existing test assertion weakened or removed
5. Correct test type per layer — no `@SpringBootTest` for feature work
6. Event assertions use `times(n)`, not bare `any()`
7. No test whose only assertion is `isNotNull()`, `isNotEmpty()`, or a status code
8. Deterministic fixtures — no `Instant.now()` in an asserted field, no system-clock dependency

### Dimension 4 checklist (15%)

1. `clean verify -Pharness-gates` exits 0
2. Zero Checkstyle violations
3. Zero SpotBugs findings at `HIGH` or `MEDIUM`
4. No new Maven dependency
5. No unrelated reformatting in the diff

---

## Verdict thresholds

| Total | Verdict | Orchestrator action |
|---|---|---|
| Any hard gate failed | `FAIL` | Retry, or escalate at iteration 3 |
| ≥ 85 | `PASS` | Archive, advance |
| 70–84 | `CONDITIONAL PASS` | Archive, advance, append open items to `debt-log.md` |
| < 70 | `FAIL` | Retry, or escalate at iteration 3 |

`CONDITIONAL PASS` exists because a sprint can be architecturally sound, feature-complete, and still
have a thin test or a mislabelled layer. Retrying for that burns an iteration against the escalation
budget and risks a regression in code that is already correct. The band is narrow on purpose — it is
not a place to put anything a hard gate should have caught.

**`CONDITIONAL PASS` requires all eleven hard gates to have passed.** There is no path from a gate
failure to a conditional pass. This is the single most important property of the framework: the band
that allows discretion sits entirely above the gates, never around them.

---

## Determinism

### Ambiguity fallback

When a check cannot be resolved to PASS or FAIL — tool did not run, output unreadable, an AC genuinely
open to two readings, a cited test that half-asserts the observable:

1. Record the check as **FAIL**.
2. Prefix the gate result line with `UNDETERMINED:` and state what could not be decided.
3. In `## Required fixes`, name the fix as making the check decidable — a clarified AC, a tool that
   runs, an assertion that maps one-to-one onto the observable.

Fail closed, every time. Never average, never take the midpoint, never resolve ambiguity with a
`CONDITIONAL PASS`. An `UNDETERMINED` count above zero on two consecutive sprints is a signal that a
contract or a skill file is unclear, and the Monitor will surface it.

### Worked example — variable output to a definitive verdict

Sprint 1 of the shift handover feature, iteration 1. The Generator produced a service with
partial-failure handling and five tests, and claimed all six ACs satisfied.

**Gates:**

```
./mvnw -q clean verify -Pharness-gates  → exit 0
ArchitectureRulesTest                    → 9 rules, 0 violations
Checkstyle                               → 0 violations
SpotBugs                                 → 0 HIGH/MEDIUM
jacoco:check                             → service 84%, web n/a, overall 78%   → pass
ac-gates.sh                              → AC-1: PASS   AC-2: PASS   BUILD-2: PASS
```

Ten of eleven gates pass on tool output alone, `AC-1`'s citation half among them — every AC resolves
to a test method that exists, so nothing is `NOT IMPLEMENTED` and nothing is cited to a source file.
What the script cannot answer is whether those tests assert the right things. Opening each one:

- AC-1.1, AC-1.3, AC-1.4, AC-1.6 — cited tests assert on the result and on persisted state. Satisfied.
- AC-1.2 requires `failed[].reason == INVALID_TRANSITION`, the activity unchanged, no audit entry,
  and the id absent from `succeeded[]`. `BulkStatusServiceTest#rejectsTransitionFromDone` asserts
  `failed()` is non-empty and nothing else.
- AC-1.5 requires no audit entry for a failed item. The cited test asserts on the succeeded count only.

`AC-1` fails on its read half: two ACs have cited tests that do not assert their observables. A
status-only assertion on a business rule also fails `TEST-2` by construction. **Verdict: `FAIL`.**
Score is not computed. `HARD_GATES_FAILED: AC-1, TEST-2`.

The property that matters: nothing in that chain depended on the Evaluator's opinion of the code. A
second Evaluator run, on the same diff, reaches the same place because AC-1.2 names four observables
and the cited test asserts one of them. That is a countable fact, not a judgement.

Iteration 2, same sprint. The Generator adds the missing assertions. Gates all pass, `AC-1` now
passes, and scoring runs:

```
D1 architectural   7/7 → 100 × 0.40 = 40.0
D2 acceptance      5/6 →  83 × 0.25 = 20.75    (layer label wrong on one file)
D3 test quality    7/8 →  87 × 0.20 = 17.4     (no negative assertion on the idempotency rule)
D4 build           5/5 → 100 × 0.15 = 15.0
                                total = 93.15 → 93
```

93 ≥ 85 → **`PASS`**. The two shortfalls go in `## Advisory`; they did not fail a gate, and they are
not worth an iteration.

---

## What the framework does not do

- It does not score the feature's value. That was decided at the approval gate.
- It does not grade the contract. A weak AC produces a weak gate; note it in Advisory, grade what is
  there, and fix it in the next spec.
- It does not accumulate credit across iterations. Iteration 3 is graded on iteration 3's code.
- It does not have an override. There is no way for an Evaluator to pass code that failed a hard
  gate, which is the point — the escalation path exists precisely so that discretion lives with a
  human, in the open, on the record.
