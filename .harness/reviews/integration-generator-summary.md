SPRINT: 1-3 (combined integration review)
ITERATION: 2 (combined verify attempts; no separate per-sprint iterations)
BUILD: PASS
BUILD_COMMAND: .\mvnw.cmd clean verify -Pharness-gates

This records one coordinated Codex-assisted implementation against the three sprint contracts and
`.harness/output/integration-contract.md`. It does not claim three sequential passing sprint
commits or a standalone Claude Code Evaluator `APPROVED` verdict. The integration feature diff is
against documentation base `444b8dc`; the same session's staff lookup prerequisite was
committed earlier in foundation commit `a6709b9`.

The first full verify ran 145 tests with no failures or errors and passed all nine ArchUnit tests,
but **failed** at SpotBugs with seven `EI_EXPOSE_REP` findings. After defensive list-copy fixes,
the second clean verify exited 0: 146 tests, zero failures/errors/skips, nine ArchUnit tests, zero
Checkstyle violations, zero SpotBugs findings, and all configured JaCoCo checks met. No coverage
percentage or independent sprint build verdict is inferred from that final check.

## AC self-check

| AC | Claim | Evidence | Layer |
| --- | --- | --- | --- |
| AC-1.1 | Three eligible items succeed in order with previous/new statuses and persisted targets. | `BulkStatusServiceTest#appliesEveryValidTransitionAndAuditsEachOnce` | service |
| AC-1.2 | One audit entry for the transition records status pair, actor, note, and fixed operation time. | `BulkStatusServiceTest#appliesEveryValidTransitionAndAuditsEachOnce` | service |
| AC-1.3 | DONE-to-BLOCKED is only a failed `INVALID_TRANSITION` item; status and audit remain unchanged. | `BulkStatusServiceTest#reportsIndependentFailuresWithoutChangingOrAuditingThoseItems` | service |
| AC-1.4 | A repeated id succeeds once, then fails `ALREADY_IN_STATUS`, with DONE persisted and one audit. | `BulkStatusServiceTest#repeatedItemDoesNotCreateASecondAuditEntryOrSuccess` | service |
| AC-1.5 | A valid id succeeds and persists with one audit while an unknown id fails `NOT_FOUND` without audit. | `BulkStatusServiceTest#reportsIndependentFailuresWithoutChangingOrAuditingThoseItems` | service |
| AC-1.6 | A local id succeeds while a foreign-store id fails `FORBIDDEN` with foreign state/audit unchanged. | `BulkStatusServiceTest#reportsIndependentFailuresWithoutChangingOrAuditingThoseItems` | service |
| AC-1.7 | TODO and IN_PROGRESS targets fail `UNSUPPORTED_TARGET_STATUS` without state or audit writes. | `BulkStatusServiceTest#rejectsBothUnsupportedHandoverTargetsWithoutWrites` | service |
| AC-1.8 | Two existing foreign-store ids trigger whole-request `FORBIDDEN` with both statuses/audits unchanged. | `BulkStatusServiceTest#allForeignItemsFailTheWholeRequestWithoutChangingState` | service |
| AC-1.9 | A missing actor triggers whole-request `UNAUTHORIZED` without a transition or audit. | `BulkStatusServiceTest#missingActorFailsBeforeAnyTransitionOrAudit` | service |
| AC-1.10 | An all-rejected batch returns ordered id/reason pairs with no task save or audit append. | `BulkStatusServiceTest#orderedAllFailedBatchDoesNotSaveAnyTaskOrAudit` | service |
| AC-2.1 | All-success PATCH returns 200 with counts and success id/status fields. | `BulkStatusControllerTest#returns200WithCountsAndAuditableTransitionShape` | web |
| AC-2.2 | Mixed PATCH returns 207 with both counts and success/failure id/reason fields. | `BulkStatusControllerTest#returns207WithBothOutcomeLists` | web |
| AC-2.3 | All-failed PATCH returns 207 with requested/failed counts and every failed id/reason. | `BulkStatusControllerTest#returns207WhenEveryItemHasFailed` | web |
| AC-2.4 | Empty, over-200, blank-id, null-target, and over-500-note bodies each return 400 `VALIDATION_FAILED` before service invocation. | `BulkStatusControllerTest#rejectsAllFiveInvalidBodyShapesBeforeCallingService` | web |
| AC-2.5 | Missing and invalid bearer tokens return 401 `UNAUTHORIZED` without service invocation. | `BulkStatusControllerTest#rejectsMissingAndInvalidBearerBeforeCallingService` | web |
| AC-2.6 | The token-resolved actor reaches the service despite a different actor id in the body; the response follows its result. | `BulkStatusControllerTest#returns200WithCountsAndAuditableTransitionShape` | web |
| AC-3.1 | A mixed result publishes exactly one event with actor, store, successful ids only, and operation time. | `BulkStatusServiceTest#reportsIndependentFailuresWithoutChangingOrAuditingThoseItems` | service |
| AC-3.2 | An all-rejected result publishes no event and appends no audit. | `BulkStatusServiceTest#allRejectedItemsCauseNoAuditAndNoEvent` | service |
| AC-3.3 | The read-only lookup returns only active same-store department leads and leaves all staff records unchanged. | `StaffServiceImplTest#findsOnlyActiveDepartmentLeadsInTheRequestedStore` | service |
| AC-3.4 | The event persists exactly one `SHIFT_HANDOVER` `IN_APP` alert for the eligible lead with store and count. | `ShiftHandoverListenerTest#persistsExactlyOneIncomingLeadAlertForACompletedHandover` | service |
| AC-3.5 | With no eligible lead, no alert is created and the applied task status/audit remain committed. | `ShiftHandoverIntegrationTest#missingLeadDoesNotReverseAnAppliedTransitionOrAudit` | service |
| AC-3.6 | A typed alert-write failure does not escape; the successful id, stored DONE status, and audit remain. | `ShiftHandoverIntegrationTest#failedAlertDoesNotReverseAnAppliedTransitionOrAudit` | service |

## Files changed

The following files are in the post-foundation integration feature diff. The activity interface,
single-item command/request, and existing-route tests also reflect the co-delivered principal and
TODO-to-IN_PROGRESS repairs disclosed in the integration contract.

| Path | Reason | Layer |
| --- | --- | --- |
| `src/main/java/com/cognizant/storeops/activities/service/ActivityService.java` | Expose authenticated bulk operation and actor-aware existing operations. | service |
| `src/main/java/com/cognizant/storeops/activities/service/ActivityServiceImpl.java` | Apply per-item transitions, audit, access rules, and handover event; secure existing operations. | service |
| `src/main/java/com/cognizant/storeops/activities/service/UpdateActivityCommand.java` | Remove caller-supplied acting user from existing update command. | service |
| `src/main/java/com/cognizant/storeops/activities/service/BulkStatusCommand.java` | Carry actor and ordered requested items. | service |
| `src/main/java/com/cognizant/storeops/activities/service/BulkStatusResult.java` | Represent ordered success and failure outcomes. | service |
| `src/main/java/com/cognizant/storeops/activities/web/ActivityController.java` | Route bulk PATCH and use authenticated principal on activity routes. | web |
| `src/main/java/com/cognizant/storeops/activities/web/UpdateActivityRequest.java` | Remove body acting-user field from existing PATCH. | web |
| `src/main/java/com/cognizant/storeops/activities/web/BulkStatusUpdateRequest.java` | Validate one to 200 nested updates and map them to a command. | web |
| `src/main/java/com/cognizant/storeops/activities/web/BulkStatusUpdateResponse.java` | Map per-item results and counts to the HTTP response. | web |
| `src/main/java/com/cognizant/storeops/shared/event/ShiftHandoverCompletedEvent.java` | Define the cross-module handover event payload. | events |
| `src/main/java/com/cognizant/storeops/alerts/service/listener/ShiftHandoverListener.java` | Select an eligible lead and create one in-app alert. | service |
| `src/test/java/com/cognizant/storeops/activities/service/ActivityServiceImplTest.java` | Verify actor/store and existing transition repairs. | tests |
| `src/test/java/com/cognizant/storeops/activities/service/BulkStatusServiceTest.java` | Verify service outcomes, audit, access, and event payload. | tests |
| `src/test/java/com/cognizant/storeops/activities/service/ShiftHandoverIntegrationTest.java` | Verify missing-lead and alert-failure isolation after writes. | tests |
| `src/test/java/com/cognizant/storeops/activities/web/ActivityControllerTest.java` | Verify existing route behavior with authenticated principal. | tests |
| `src/test/java/com/cognizant/storeops/activities/web/BulkStatusControllerTest.java` | Verify bulk HTTP status, response, validation, and actor source. | tests |
| `src/test/java/com/cognizant/storeops/alerts/service/listener/ShiftHandoverListenerTest.java` | Verify recipient selection, persisted alert, and listener failure handling. | tests |

The same assisted session's foundation commit `a6709b9` contains the staff lookup prerequisite,
which is part of the combined acceptance evidence but not the post-foundation feature diff:

| Path | Reason | Layer |
| --- | --- | --- |
| `src/main/java/com/cognizant/storeops/staff/repository/UserRepository.java` | Declare store-scoped staff lookup. | repository |
| `src/main/java/com/cognizant/storeops/staff/repository/InMemoryUserRepository.java` | Implement read-only store lookup. | repository |
| `src/main/java/com/cognizant/storeops/staff/service/StaffService.java` | Expose eligible-lead lookup at the module boundary. | service |
| `src/main/java/com/cognizant/storeops/staff/service/StaffServiceImpl.java` | Filter active department leads for one store. | service |
| `src/test/java/com/cognizant/storeops/staff/repository/InMemoryUserRepositoryTest.java` | Verify store filtering. | tests |
| `src/test/java/com/cognizant/storeops/staff/service/StaffServiceImplTest.java` | Verify active/role/store filtering without staff writes. | tests |

Working review artifacts in `.harness/output/` include the corrected `spec.md`, the combined
`integration-contract.md`, this summary, and the preserved first-failure/final verify logs.

## Events emitted

| Publisher | Event | Condition and consumer |
| --- | --- | --- |
| `ActivityServiceImpl#bulkUpdateStatus` | `ShiftHandoverCompletedEvent` | One publish after a bulk result with at least one success; `alerts` consumes it through `ShiftHandoverListener#on`. The payload contains store, authenticated actor, successful ids in order, and completion time. |

The bulk operation emits no per-item `TaskBlockedEvent`; that existing event belongs to the
single-activity update path.

## Known gaps

- There is no incoming-shift roster. The demonstration chooses one active department lead in the
  event's store other than the outgoing actor; with none available, no alert is written.
- AC-3.6 is directly tested for a typed `ConflictException` from alert creation and for a common
  `IllegalStateException` in the listener test. The listener catches those and
  `IllegalArgumentException`, but another unchecked failure could still escape synchronous event
  delivery. The broadest reading of the AC is therefore not fully guaranteed.
- The existing single-activity PATCH and other activity operations received principal/store
  authorization changes, and single-activity TODO-to-IN_PROGRESS was enabled. These same-session
  baseline repairs extend the original sprint contracts' stated feature scope; the combined
  integration contract discloses that expansion.
- No standalone Claude Code Evaluator approval or independent per-sprint build evidence was
  produced. The first combined verify failed SpotBugs, and only the final combined verify passed.
