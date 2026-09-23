# how-to-review

**Purpose.** The procedure an Evaluator follows on a sprint, and the standard of evidence a finding
must meet. Read by the Evaluator only. Scoring arithmetic and verdict thresholds are in
`evaluation-criteria`; this file is about how to look.

## Standing assumption

`generator-summary.md` is a claim, not a report. It was written by the agent whose work is being
graded, before anyone checked it. Treat every row in the AC self-check table as a hypothesis with a
named test you are about to open.

The Generator is not dishonest. It is optimistic in a specific, predictable way: it marks an AC
satisfied because it wrote code intended to satisfy it. Your job is the gap between intent and
behaviour.

## Order

Strict, because it controls cost and prevents a wasted review of code that is already failing.

### 1. Automated gates

Run the commands in `evaluation-criteria` — the Maven lifecycle and `.harness/checks/ac-gates.sh`.
Capture raw output. Do not read the diff yet: reading code first creates an impression that then
colours how you read a tool result, and eight of the eleven gates do not need you at all.

If any hard gate fails: verdict `FAIL`, write `## Gate results` and `## Required fixes` with the tool
output, stop. No dimension scores, no AC verification, no advisory notes. A FAIL review that also
contains 200 lines of style commentary buries the three lines the Generator needs.

### 2. Contract-to-diff coverage

Only if all hard gates pass.

`ac-gates.sh` has already decided the mechanical part: `AC-2` scope, and whether `pom.xml` or a
governance file is in the diff. Do not re-derive its answer by reading paths yourself — quote its
line. What is left for you here is the part a path cannot show:

- Does the `Files changed` table's layer column match each file's actual package? A file in
  `activities/web/` labelled `service` is either a mislabel or a misplacement; open it and find out
  which.
- Is anything in the contract's `Out of scope for this sprint` present in the diff? Scope by module
  and layer is mechanical; scope by *behaviour* is not — sprint 1 implementing sprint 2's validation
  inside the service passes the path check and violates the contract.

### 3. AC verification

`ac-gates.sh` has proven that every AC has a row and that every citation resolves to a test method
that exists. That is the half you no longer have to check. This is the other half, and it is the
reason the gate is not fully automated.

For each AC in the contract, in order:

1. Read the AC's THEN clauses and list the observables it names.
2. Open the test method cited as evidence.
3. Confirm that test asserts on every observable in the THEN — including the negative clauses.
4. Confirm the test would fail if the behaviour were absent. A test asserting `isNotNull()` on a
   result passes whether or not the rule holds.

An AC is `satisfied` only when a cited test asserts on its observables. `not verifiable` when the
cited test exists but asserts something else, or when the evidence field says `NOT IMPLEMENTED`.
`not satisfied` when the behaviour is demonstrably absent from the diff.

The most common real finding, and the one to look for first: **the test asserts the status code the AC
mentions and nothing else.** That is failure mode 3. It is not a style issue and it is not advisory —
the AC is not satisfied.

### 4. Architecture review of the diff

ArchUnit has already proven the mechanical half of ARCH-1, ARCH-2, ARCH-3, and ARCH-4. Read the diff
for the half it cannot prove:

- **ARCH-2** — is there a cross-module write routed around the event bus in a way package rules miss?
  A shared interface used as indirection, a listener placed in the publishing module, a `publish` call
  that passes a callback.
- **ARCH-3** — has a business rule leaked into a controller? A controller deciding *which items* are
  eligible, rather than which HTTP status to map a result to, is a violation. A controller mapping
  `result.failed().isEmpty()` to 200 versus 207 is not.
- **ARCH-4** — any `save`, `update`, `delete`, or `publish` in a `reports` diff, on any type not owned
  by `reports`.
- **ERR-1** — a `catch (Exception e)` that swallows and returns a default. Checkstyle catches the
  throw; it does not catch a service that quietly absorbs a failure and reports success. Check what
  happens in the catch block.

### 5. Test quality

Beyond the AC-level check: are negative assertions present for each business rule? Is
`verify(..., never())` used where the rule says something should *not* happen? Were existing test
assertions modified? Is coverage sitting suspiciously close to a threshold on a new service class —
which usually means tests were written to the number rather than the behaviour?

### 6. Score and write

Only now. Apply `evaluation-criteria` arithmetic, then write the file in the order the agent
definition specifies.

## Standard of evidence for a finding

Every entry in `## Required fixes` needs three things:

1. **Location** — `src/main/java/com/cognizant/storeops/activities/service/ActivityServiceImpl.java:88`
2. **The rule or AC** — a gate ID, or an AC number, or a named convention
3. **What correct looks like** — specific enough to act on without asking a human

Compare:

> Tests are insufficient.

against

> `BulkStatusServiceTest#rejectsTransitionFromDone:41` asserts only that `failed()` is non-empty.
> AC-1.2 requires the failure reason to be `INVALID_TRANSITION` and requires the activity's status to
> remain `DONE`. Add assertions on `failed().get(0).reason()` and on the task's status after the call,
> plus `verify(taskRepository, never()).save(any())`.

The second is actionable by an agent with no human in the loop, which is the requirement — the
Generator's next iteration has only this file to work from. If a finding cannot be written to that
standard, either it is not a finding or you have not finished investigating it.

## Line references

Cite the line in the version you reviewed, and cite the commit or diff you reviewed against. On
iteration 2 and 3 the line numbers will have moved; a reference without a commit is noise. Where a
finding is about an absence rather than a line, cite the file and say what is missing.

## Tone

Findings describe the code, not the author. No praise, no encouragement, no "great work on the
partial-failure handling". The Generator does not benefit from it and it inflates a file whose only
purpose is to be acted on. Equally, no editorialising about how bad a violation is — the gate already
encodes severity.

## What is not the Evaluator's job

- Fixing anything. Do not include a patch or a diff in the feedback file.
- Adding requirements the contract did not contain. Those go in `## Advisory`, and if they matter they
  belong in the next spec.
- Grading the contract. If the contract's ACs are weak, that is a Planner problem; note it in Advisory
  and grade against what is there.
- Judging style. `coding-conventions` is deliberately not in your skill reads. If style is genuinely
  broken, Checkstyle failed and the build gate already caught it.
- Reconsidering a previous verdict. Each iteration is graded on its own inputs. An improvement from
  iteration 1 earns no credit; the code either passes now or it does not.
