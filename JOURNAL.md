# Architecture journal

Decisions, trade-offs and reversals recorded while building the harness. Kept rough on purpose —
these are working notes, not a second design brief. `DESIGN_BRIEF.md` states what the harness does
and why; this file records how the reasoning actually went, including the parts that changed.

---

## Entry 1 — Splitting skill files by reader, not by subject

The obvious organisation was by topic: one architecture file, one testing file, one API file. I went
with a split by *which agent reads it* instead, and the forcing question was whether the Evaluator
should read `coding-conventions`.

It should not. An assessor holding a style guide mines it for findings, and style is not a gate —
Checkstyle already failed the build if it mattered. Once that was clear the same logic ran the other
way: the Generator must not read `evaluation-criteria`, because an implementer that can read the
rubric optimises for the rubric rather than the contract, and the gap between those two is the entire
signal the Evaluator exists to detect.

The consequence I did not anticipate: `architecture-principles` has to be *shared verbatim* between
Generator and Evaluator. Two paraphrases of the same rule make the loop oscillate — the Generator
writes what its version permits, the Evaluator fails what its version forbids, and no iteration
converges. So the rule text is single-sourced and the gate IDs never get renumbered.

**Open question:** `app-context` is read by all four agents and will grow as StoreOps does. At some
size it becomes cheaper to split it per module and have each agent read only the modules its sprint
touches. I have not worked out where that threshold is.

## Entry 2 — Rejecting Spring Modulith

Modulith is the better answer to module boundaries than ArchUnit. It is purpose-built, the boundary
is a first-class concept rather than a package-name convention, and it would have made ARCH-1 and
ARCH-2 nearly free.

I rejected it on scope, and I want the reasoning on the record because it is the kind of call that
looks like timidity later. Adopting Modulith changes the application's architecture — annotations in
production code, a module descriptor, a different way of declaring the event bus. The harness is a
governance layer over a client's codebase. It is not entitled to restructure the codebase it governs
on the client's behalf, and a standards team already nervous about AI-assisted development is exactly
the wrong audience for a harness that arrives having rewritten their module system.

ArchUnit sits entirely in the test tree. If the squad throws the harness away, nothing in `src/main`
changes.

**The cost of that choice**, stated plainly: every ArchUnit rule here depends on the package layout
`com.cognizant.storeops.<module>.<layer>`. A move to vertical-slice packaging breaks all of them, and
it breaks them *silently* — the rules keep passing, they just stop meaning anything. Modulith would
have failed loudly. This is the assumption I would check first if the harness ever seemed too quiet.

## Entry 3 — The `BUILD-2` gate came last and matters most

I added `BUILD-2` (no new suppressions) after writing the other ten gates, and only because I asked
what the cheapest way to pass each one was. For every single gate the answer was the same:
`@SuppressWarnings`, `@Disabled`, a JaCoCo exclusion, or two characters in `pom.xml`.

Ten carefully reasoned gates, all bypassable by one annotation. `BUILD-2` is the gate that protects
the gates, and if I could keep only one it would be this one — every other gate assumes the checks are
actually running.

It also changed how I wrote `generator.agent.md`. The constraint is stated there as an absolute with
its escape route named in the same breath: if a gate is genuinely wrong, say so in `Known gaps` and
let it fail, because escalation exists for that. A rule with no legitimate way to disagree with it
gets worked around rather than followed.

## Entry 4 — The dimension I had left un-gated

Reading back over `evaluation-criteria` with the four dimensions side by side, dimension 2 was the
odd one out and I had not noticed it while writing them one at a time. D1 has ArchUnit, D3 has
JaCoCo, D4 has the Maven lifecycle. D2 had `AC-1` and `AC-2`, and *both* were "the Evaluator reads
something". So the two gates that connect the verdict to the contract — the pair whose whole job is
to stop the loop terminating on a feature that was never built — were the two with no tool behind
them.

That is backwards. Everywhere else in the design I argued that the gates protecting the most
important property should be the least dependent on an assessor's mood, and then I left the most
important property to an assessor.

What I could not do is automate the gate as written. "Does the cited test assert the AC's
observables" is a semantic question; a script cannot read `rejectsTransitionFromDone` and decide
whether asserting `failed()` non-empty covers four THEN clauses. What I could do is split the gate
and automate the countable half. `.harness/checks/ac-gates.sh` now decides, by exit code: does every
AC have a row in the summary, does its Evidence cell parse as `Class#method`, does that method exist
in the test tree, and is every changed file inside `MODULES_TOUCHED × LAYERS`.

Writing it turned up two things I had not planned for.

First, it forced a contract change. `AC-2` is specified against the contract's `MODULES_TOUCHED`
header — and `planner.agent.md` never told the Planner to write that field. The template had it, the
Generator read it, the Evaluator graded against it, and the agent that produces the file did not know
it existed. A gate depending on a field its producer was never told to emit is a gate that would have
passed vacuously on every sprint. I only found it because a script has to parse what a human eye
skims.

Second, the same diff walk cheaply closes most of `BUILD-2`'s manual half. I had written that a
`pom.xml` threshold edit is invisible to every linter, which is true, and concluded it had to be an
Evaluator diff scan, which does not follow. A script cannot judge whether a threshold change is
*legitimate*, but it can certainly notice `pom.xml` is in the diff at all — and per my own rule, if
the contract did not ask for it, that is already a FAIL. So the automated part is the whole part in
practice.

**What I would watch.** The split means `AC-1` now reports two results, and an Evaluator that quotes
only the script's `AC-1: PASS` has made the gate weaker than before, because the passing line reads
like the gate passed. The feedback template now shows both halves on one row for exactly this
reason. If I see a verdict citing the script alone, the fix is in `how-to-review`, not here.

## Entry 5 — Two gates that were never going to fire

Found by re-reading the check configuration rather than by running it, which is the uncomfortable
part.

`checkstyle-suppressions.xml` scoped ERR-1 to services and routes with
`<suppress checks="ERR-1-rawThrow" files="...(domain|repository)..."/>`. Checkstyle matches the
`checks` attribute against the violation's *source name* — the check class,
`RegexpSinglelineJavaCheck` — and matches the module `id` with a separate `id` attribute. So the
scoping matched nothing. Two consequences, both worse than the bug: ERR-1 would have fired on domain
and repository classes that are outside the rule as written, and `shared/error` would not have been
exempt, so `GlobalExceptionHandler` catching `Exception` fails the build on the baseline commit.
A one-word fix, `checks` to `id`, in a file whose own comment explained precisely why the scoping
mattered.

Then surefire. The profile pinned `<skipTests>false</skipTests>` and the comment claimed the
architecture gates could never be skipped. `-DskipTests` is indeed neutralised by that. But
`-Dmaven.test.skip` is a different parameter — it skips test *compilation* — and it was unpinned.

Both of these are the same failure the harness exists to prevent, one level up: a check that reads as
covering something and does not fire. I wrote that sentence into `checkstyle-build2-tests.xml` as an
argument for splitting the Checkstyle configs, and then shipped two instances of it in the file next
door.

## Entry 6 — Running the gates, and what reading them had missed

I had written in the entry above that the gates are the only code here with no test, called that the
lesson, and then done the thing I had just criticised: added two manual verification notes to a
README and moved on. So I installed a JDK and Maven and built the fixture project instead.

It found two more defects and one gate conflict, none of which I would have got to by reading.

**The surefire fix was wrong.** Not incomplete — wrong. I had pinned `<skip>false</skip>` on surefire
and written a confident comment saying that closed `-Dmaven.test.skip`. Measured:
`mvn verify -Pharness-gates -Dmaven.test.skip=true` → `BUILD SUCCESS`, exit 0, zero tests run, nine
ArchUnit rules never loaded, `jacoco:check` passing having analysed nothing. `maven.test.skip` is
read by `maven-compiler-plugin`'s `testCompile` as well, so the test tree never compiles; surefire
then finds nothing and reports success, and JaCoCo skips for want of an execution-data file. Two
vacuous passes in series. It takes three plugins to close, and a `failIfNoTests` backstop so that
"zero tests" is an error regardless of which property a future Maven reads. I had the right
instinct, the right mechanism, and the wrong number of plugins, and only one of those three things
is visible without running it.

**ArchUnit fails a rule that has nothing to check.** `layeredArchitecture()` reports an empty layer
as a violation and `failOnEmptyShould` fails any rule whose `that()` clause matches no classes. On a
tree with no `web` and no `repository` classes, four of the nine rules fail — with messages about
ArchUnit configuration, which the Evaluator would have quoted verbatim as an architecture violation
and routed to the Generator. An iteration burned, out of three, on code that was correct, with
feedback the Generator cannot act on. Fixed with `optionalLayer` and `allowEmptyShould(true)` on the
module-scoped rules, and I want the counter-argument on the record because it is not obviously wrong:
ArchUnit's default *is* fail-closed, and fail-closed is this harness's own rule. I still think it is
wrong here. "No `reports` class writes to another module's repository" is true of a codebase with no
`reports` classes, and a gate whose FAIL does not correspond to anything a human would call a
violation is a gate that trains people to ignore it.

**Two gates disagreed with each other.** SpotBugs at `threshold=Medium` reports
`CT_CONSTRUCTOR_THROW` on a domain entity that validates its arguments — the exact code ERR-1's
suppressions deliberately permit. So `BUILD-1` failed, `BUILD-2` forbade suppressing it, and nothing
in any skill file told the Generator which of the two rules won. That is the "contract asking for two
incompatible things" case from the escalation template, except it was not in a contract, it was in my
own gate configuration, and it would have fired on the first entity the Generator wrote. Neither
file is wrong when read on its own, which is why reading them was never going to find it.

**What I would take to a client from this.** The order was backwards for the whole build. I wrote
eleven gates, reasoned carefully about what each one prevents, documented why none can be advisory —
and ran none of them until the configuration was finished. Three of the defects produced a *green*
build, which means the failure mode of an untested gate is not that it breaks, it is that it silently
blesses. If I did this again the gate self-test would come second, immediately after the first gate,
and `fixtures/gate-selftest.sh` would be in CI next to `mvn verify`. Ten assertions, one command, and
it runs in about ninety seconds.

I have left the two manual verification notes out of `.harness/checks/README.md` now. They were a
substitute for a test, and the substitute would have been performed once, by me, and then believed
for the rest of the harness's life.
