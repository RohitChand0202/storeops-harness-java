# .harness/checks/

The gate definitions. Eight of the eleven hard gates in `evaluation-criteria` are decided entirely
by a tool configured here, and two more are decided here in part, which is why these files live
together rather than scattered through the build.

| File | Gates | Where it goes |
|---|---|---|
| `ArchitectureRulesTest.java` | ARCH-1..4, ERR-1 (backstop) | Copy to `src/test/java/com/cognizant/storeops/architecture/` |
| `checkstyle-harness.xml` | ERR-1 (primary), BUILD-2 main sources, conventions | Referenced in place by the Maven profile |
| `checkstyle-suppressions.xml` | Scopes ERR-1 to service and web paths | Referenced in place |
| `checkstyle-build2-tests.xml` | BUILD-2 test sources — disabled tests, commented-out ArchUnit rules | Referenced in place |
| `pom-harness-gates.xml` | BUILD-1, TEST-1, and the wiring for all of the above | Paste the `<profile>` into `pom.xml`'s `<profiles>` block |
| `ac-gates.sh` | AC-2, AC-1 (citation half), BUILD-2 (diff-scan half) | Run in place by the Evaluator |
| `verify-submission.sh` | None — pre-submission check on the artefact chain | Run in place |

`ac-gates.sh` exists because dimension 2 was the one dimension with no automated gate behind it.
`AC-1` and `AC-2` are the two gates that connect the verdict to the contract, and leaving both to an
LLM assessor put the most important link in the framework on the least deterministic footing. The
script decides what is countable — does every AC have a row, does its citation resolve to a real test
method, is every changed file inside the declared module × layer scope, is `pom.xml` in the diff —
and leaves the Evaluator only the judgement that genuinely needs reading: whether an assertion maps
onto the observable its THEN clause names.

Two Checkstyle configurations rather than one, because `includeTestSourceDirectory=false` is needed to
keep the convention checks off the test tree, and that same setting makes a test-source `@Disabled`
rule unmatchable. One config would give a BUILD-2 that reads as covering disabled tests and never
fires on one.

## Setup

```bash
mkdir -p src/test/java/com/cognizant/storeops/architecture
cp .harness/checks/ArchitectureRulesTest.java src/test/java/com/cognizant/storeops/architecture/
chmod +x .harness/checks/ac-gates.sh .harness/checks/verify-submission.sh
# then paste pom-harness-gates.xml into <profiles> in pom.xml
./mvnw -q clean verify -Pharness-gates
```

Two things to verify once, on the first real sprint, because both fail silently rather than loudly:

1. **The per-package JaCoCo rules bind.** Drop one service class's tests and confirm the failure
   names the package, not just the bundle. See the comment above the JaCoCo plugin in
   `pom-harness-gates.xml` for why the include patterns are listed in two forms.
2. **ERR-1 is scoped where the rule says it is.** A `throw new IllegalStateException` in a `domain`
   class must *not* fail the build, and one in a `service` class must. Checkstyle matches the
   `checks` attribute against the check class and `id` against the module id, so scoping by `checks`
   silently matches nothing — see the note in `checkstyle-suppressions.xml`.

`ArchitectureRulesTest.java` has to be copied because ArchUnit rules are JUnit tests and Surefire
only picks up `src/test/java`. The source of truth stays here so the harness's gate definitions are
reviewable in one place; the copy in the test tree is what runs. If you change one, change both —
the Evaluator quotes rule names from the running test, and a drift between the two means evaluator
feedback cites a rule that no longer exists.

## Why `.harness/` and not `.github/`

Harness configuration and pipeline configuration are different artefacts with different lifecycles.
`.harness/` is governance: agents, skill files, gate definitions, the review archive. `.github/` is
delivery: what runs on push, on PR, on merge to `main`.

They share exactly one thing, deliberately: the `harness-gates` Maven profile. The pipeline activates
it unchanged.

```yaml
# .github/workflows/ci.yml — the relevant step
- name: Harness gates
  run: ./mvnw -B clean verify -Pharness-gates
```

Thresholds are defined in `pom-harness-gates.xml` and nowhere else. If service coverage moves from
80% to 85%, it moves in that one file and both the harness and CI read it from there. The failure
this prevents is the one that makes a harness worthless: local gates drifting looser than pipeline
gates, so a harness PASS stops predicting a CI pass and nobody notices until a merge breaks.

The harness **precedes and duplicates** CI. It does not replace it. CI remains the enforcing
authority on `main`, because the harness runs on a developer machine and can be skipped, run stale,
or run against uncommitted work. A harness PASS is a prediction; CI is the fact.

**If CI fails after a harness PASS, that is a harness defect.** Record it in
`.harness/reviews/debt-log.md` and fix it ahead of any feature work. The harness has exactly one
property worth having, which is that a PASS means something.
