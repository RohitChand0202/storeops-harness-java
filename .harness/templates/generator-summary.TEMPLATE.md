# Template — `generator-summary.md`

Written by the Generator to `.harness/output/` after it has run the build itself. Section headings
are exact — the Evaluator reads them by name.

---

```
SPRINT: <n>
ITERATION: <i>
BUILD: PASS | FAIL
BUILD_COMMAND: ./mvnw -q clean verify -Pharness-gates
```

## AC self-check

| AC | Claim | Evidence | Layer |
|---|---|---|---|
| AC-1.1 | Three IN_PROGRESS activities transition to DONE and appear in succeeded[] | `BulkStatusServiceTest#marksEligibleActivitiesDone` | service |
| AC-1.2 | DONE→BLOCKED rejected with INVALID_TRANSITION, nothing persisted | `BulkStatusServiceTest#rejectsTransitionFromDone` | service |
| AC-1.5 | All-fail request persists nothing and writes no audit entry | `BulkStatusServiceTest#persistsNothingWhenEveryItemFails` | service |
| AC-1.6 | — | NOT IMPLEMENTED | — |

`Evidence` is a test method reference (`Class#method`) or the literal `NOT IMPLEMENTED`. Never a
source file — a source file shows intent, a test shows behaviour. Never a test you did not run.

## Files changed

| Path | Reason | Layer |
|---|---|---|
| `src/main/java/.../activities/service/ActivityServiceImpl.java` | bulkUpdateStatus operation | service |
| `src/main/java/.../activities/service/BulkStatusResult.java` | per-item outcome record | service |
| `src/main/java/.../activities/domain/AuditEntry.java` | audit record per transition | domain |
| `src/test/java/.../activities/service/BulkStatusServiceTest.java` | AC coverage | test |

The Evaluator checks the layer column against each file's actual package, so a wrong label here is a
caught error rather than a hidden one.

## Events emitted

NONE — sprint 1 does not cross a module boundary.

Write the section even when it is empty. Omitting it reads as an oversight and the Evaluator cannot
tell the difference between "no events" and "forgot to report events".

## Known gaps

- AC-1.6 (`ALREADY_IN_STATUS`) not implemented. The contract does not say whether an idempotent
  repeat should count as a failure or a no-op success, and the two produce different audit records.
  Implemented nothing rather than guessing.
- `coding-conventions` specifies constructor injection; the existing `ActivityServiceImpl` uses field
  injection with `@Autowired`. Followed the existing file rather than refactoring it, per the
  precedence rule in the agent definition.

Declare gaps. A declared gap costs one check under dimension 2. A gap the Evaluator finds costs that
check *and* the AC, and burns one of three iterations.
