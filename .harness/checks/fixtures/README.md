# .harness/checks/fixtures/

A test for the gates.

Everything the harness governs is checked by something. The gates themselves were checked by nobody,
and it showed: three defects in them were found by reading the configuration, and every one produced
a **green build with a gate silently not firing** — the precise failure mode this harness exists to
prevent, one level up from where it prevents it. A gate that cannot fail is indistinguishable from a
gate that passes, and `BUILD-1` exiting 0 was being read as evidence that all nine tool-decided gates
had run.

`gate-selftest.sh` assembles this project, splices in the real `<profile>` from
`../pom-harness-gates.xml`, and asserts each gate's behaviour on source that sits deliberately on its
boundary.

```bash
.harness/checks/fixtures/gate-selftest.sh
```

Needs a JDK 17+ and Maven on `PATH`, plus network access the first time to resolve the gate plugins.
Set `MAVEN_REPO_LOCAL` to keep the resolved plugins out of `~/.m2`.

## What each file is for

Every source file here exists to sit on one side of one gate. None of it is illustrative.

| File | Sits on | Asserts |
|---|---|---|
| `activities/domain/Task.java` | ERR-1's lower bound | A domain constructor throwing `IllegalArgumentException` is **accepted**. ERR-1 covers `service` and `web`; `checkstyle-suppressions.xml` exempts `domain` and `repository`. Also the SpotBugs `CT_CONSTRUCTOR_THROW` case. |
| `shared/error/GlobalExceptionHandler.java` | ERR-1's exemption | `catch (Exception e)` inside `shared.error` is **accepted** — that package defines the error contract. |
| `shared/error/AppException.java` | ERR-1's exemption | `extends RuntimeException` is **accepted** in the one package permitted to name raw exception types. |
| `activities/service/ActivityServiceImpl.java` | TEST-1 | Four branches, one covered, so the `service` package lands at 0.22 against a 0.80 threshold and the per-package rule must fire **by name**. |
| `ActivityServiceImplTest.java` | TEST-1 | The one covering test. Deleting an assertion here changes the coverage number, which is the point. |
| *(generated at run time)* | ARCH-1..4, ERR-1 backstop | `ArchitectureRulesTest.java` is copied in from `../`, because ArchUnit rules only run from `src/test/java`. |

The script also writes and deletes a `SelfTestBadService.java` carrying a raw throw in `service/`, to
prove ERR-1 **rejects** what it is supposed to reject. A gate is only proven by a case it refuses and
a case it accepts; this directory always has both.

## What it caught

All three had shipped, and all three were green:

1. **ERR-1 was unscoped.** `checkstyle-suppressions.xml` scoped by `checks=`, which Checkstyle
   matches against the check *class* (`RegexpSinglelineJavaCheck`), not the module `id`. The
   suppressions matched nothing. Measured: identical source tree, `checks=` fails on `Task.java:10`
   and `GlobalExceptionHandler.java:11`; `id=` passes. The original would have failed `BUILD-1` on
   the baseline scaffold, before any harness run.

2. **`-Dmaven.test.skip=true` defeated every architectural gate.** Surefire's
   `<skipTests>false</skipTests>` neutralises `-DskipTests`, but `maven.test.skip` is also read by
   `maven-compiler-plugin`'s `testCompile`, so the test tree never compiled, Surefire found nothing
   and reported success, and JaCoCo skipped its check for want of an execution-data file. Measured:
   `BUILD SUCCESS`, exit 0, zero tests, nine ArchUnit rules never loaded. Closed by pinning three
   plugins, not one.

3. **ArchUnit failed four rules for having nothing to check.** `layeredArchitecture()` reports an
   empty layer as a violation, and `failOnEmptyShould` fails any rule whose `that()` clause matches
   no classes. On a tree without `web` or `repository` classes that is four FAILs describing an
   ArchUnit configuration state as an architecture violation — which the Evaluator would have quoted
   and routed to the Generator, burning an iteration on code that was correct.

And one conflict between two gates, which no amount of reading either one in isolation would have
surfaced: SpotBugs at `threshold=Medium` reports `CT_CONSTRUCTOR_THROW` on exactly the validating
domain constructor that ERR-1's suppressions deliberately permit. `BUILD-1` failed, `BUILD-2` forbade
the Generator from suppressing it, and no gate message explained which rule it was meant to obey.
See `../spotbugs-excludes.xml`.

## When to run it

- Any time a file in `.harness/checks/` changes.
- When the Checkstyle, SpotBugs, JaCoCo, ArchUnit or Surefire version in the profile moves. Several
  assertions here depend on tool behaviour that is an implementation detail and not a documented
  contract — that JaCoCo names packages with dots in a rule violation, that an explicit plugin
  `<configuration>` value beats the matching `-D` property. Both are true today and neither is
  promised.
- Before trusting a `PASS` on a codebase whose package layout has moved. `DESIGN_BRIEF.md` decision
  D2 names vertical-slice packaging as the thing most likely to break this harness *silently*; the
  "no rule failed merely because its subject package is absent" assertion is the closest thing here
  to an alarm for it.

It is not part of `mvn verify`. It tests the build configuration, so it cannot run inside the build
it tests.
