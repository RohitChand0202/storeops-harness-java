# Reflection - StoreOps harness demonstration run

## What the harness did well

The acceptance contracts made partial success measurable: they named the ordered successful and failed activity IDs, the persisted status and audit entry for each success, the absence of writes for rejected items, and the single handover event. The combined integration contract links 22 criteria to those observables. The local HTTP smoke then exercised the same path with one real activity and one missing ID: the endpoint returned `207`, counts of two requested, one succeeded, and one failed with `NOT_FOUND`; the incoming lead received one `SHIFT_HANDOVER` alert.

The automated build gates also caught a defect that a green unit suite would have missed. The first clean build ran 145 tests without a test failure, but SpotBugs failed on seven `EI_EXPOSE_REP` findings in list-bearing request, result, and event records. After defensive copies were added, a second clean `verify -Pharness-gates` passed 146 tests, including nine architecture tests, with no SpotBugs findings and all JaCoCo checks met. The two raw build logs are working files in `.harness/output/`; the final review artefacts belong in `.harness/reviews/`.

## Where it fell short

The code and contracts were produced in one coordinated Codex-assisted session. The recorded sprint contracts are a useful review breakdown, but they are not evidence of three sequential Generator/Evaluator cycles or of a standalone Claude Code `APPROVED` handoff. The spec correctly remains marked `AWAITING APPROVAL`. This run therefore did not test the central assumption in Design Brief decision **D1**: that fresh agent invocations, communicating only through files, keep the archive complete and prevent conversation context from influencing a later role.

Deployment evidence is also narrower than the case study's Docker or cloud target. A packaged JAR responded in a local Windows process, but Docker was unavailable, so there is no container start or container screenshot. The application still stores data in memory and has no shift roster; its alert recipient is an eligible active department lead in the same store, rather than an identified incoming shift lead.

## One concrete improvement

Implement a repository runner for the D1 handoff rule. For each role, it should start a fresh invocation with only that role's declared skills and input artefacts, require the expected output and gate result before advancing, and append the role, input hashes, commit hash, exit code, and timestamp to a run manifest in `.harness/reviews/`. A reviewer could then verify that the artefact-only boundaries were actually exercised, instead of inferring them from the final files.
