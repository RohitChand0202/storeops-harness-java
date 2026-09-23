# how-to-test

**Purpose.** What to test, at which layer, with which tool, and what makes an assertion count. Read
by the Generator only. The Evaluator's own view of test quality is in `how-to-review` — the two files
agree, but the Generator does not get to read the grading rubric.

This file exists mainly to prevent failure mode 3: tests that assert HTTP status codes but never check
that the business rule held.

## Test type per layer

| Layer | Tool | Style |
|---|---|---|
| `service` | JUnit 5 + Mockito | Plain unit test. Mock the repository and the event bus. No Spring context. |
| `web` | `@WebMvcTest` + MockMvc | Slice test. `@MockBean` the service. Asserts wire contract only. |
| `repository` | JUnit 5 | Direct instantiation of the in-memory impl. No mocks. |
| events | JUnit 5 + Mockito | Assert on the captured event in the publisher's test; assert the listener's effect in a separate test in the consuming module. |
| architecture | ArchUnit | `ArchitectureRulesTest` at the root. Never modify these to make a build pass. |

No `@SpringBootTest` for feature work. It is slow, it hides layering violations by wiring everything,
and a passing full-context test tells you nothing about whether `web` reached into `repository`.

## Naming

`<ClassUnderTest>Test`, methods named for the behaviour in the present tense, no `test` prefix:

```java
class BulkStatusServiceTest {
    @Test void marksEligibleActivitiesDone() { }
    @Test void rejectsTransitionFromDone() { }
    @Test void writesOneAuditEntryPerSucceededItem() { }
    @Test void publishesSingleHandoverEventForMixedResult() { }
    @Test void doesNotPublishWhenEveryItemFails() { }
}
```

Method names are the AC evidence cited in `generator-summary.md`, so a name that does not describe the
behaviour makes the summary unverifiable and costs the AC.

Use `@Nested` classes to group by scenario when a class exceeds ~10 tests. Use
`@ParameterizedTest` + `@EnumSource` for per-enum-value rules, which is the right shape for testing
status transitions across all four `TaskStatus` values.

## What a behavioural assertion is

This is the part that matters. A status-code assertion proves the endpoint answered. It does not prove
the domain did the right thing.

Not acceptable as the only assertion for a business-rule AC:

```java
@Test
void bulkUpdateReturnsOk() throws Exception {
    mockMvc.perform(patch("/api/activities/bulk-status")
                    .contentType(APPLICATION_JSON)
                    .content(validBody()))
            .andExpect(status().isOk());          // proves nothing about the rule
}
```

Acceptable — asserts the state after the call:

```java
@Test
void rejectsTransitionFromDone() {
    Task done = taskFixture("a-1042", TaskStatus.DONE);
    when(taskRepository.findAllById(List.of("a-1042"))).thenReturn(List.of(done));

    BulkStatusResult result = service.bulkUpdateStatus(
            command("a-1042", TaskStatus.BLOCKED));

    assertThat(result.failed())
            .singleElement()
            .satisfies(f -> {
                assertThat(f.taskId()).isEqualTo("a-1042");
                assertThat(f.reason()).isEqualTo("INVALID_TRANSITION");
            });
    assertThat(result.succeeded()).isEmpty();
    assertThat(done.getStatus()).isEqualTo(TaskStatus.DONE);   // unchanged
    verify(taskRepository, never()).save(any());
    verify(auditEntryRepository, never()).save(any());
}
```

Four things that test does and the bad one does not: it names the failure reason, it confirms the
entity did not change, it confirms nothing was persisted, and it confirms no audit entry was written.
The last two are the assertions that catch the bug a bulk endpoint actually ships with.

### The rule

**Every AC about a business rule needs at least one assertion on state or outcome, not only on
status.** In a MockMvc test that means asserting on `jsonPath` into the response body, not just
`status()`. In a service test it means asserting on the returned result *and* on what was or was not
persisted.

```java
.andExpect(status().isMultiStatus())
.andExpect(jsonPath("$.succeededCount").value(3))
.andExpect(jsonPath("$.failed[0].reason").value("INVALID_TRANSITION"))
```

### Negative assertions

Include at least one per business rule. `verify(repo, never()).save(any())` and
`verify(eventBus, never()).publish(any())` are the two that catch the most real defects, because the
common generated bug is doing the right thing *and also* something extra.

## Events

Capture and assert on the event, never on a downstream effect in another module — that would couple
the test across a boundary the architecture forbids:

```java
@Test
void publishesSingleHandoverEventForMixedResult() {
    service.bulkUpdateStatus(command(/* two valid, one invalid */));

    ArgumentCaptor<StoreOpsEvent> captor = ArgumentCaptor.forClass(StoreOpsEvent.class);
    verify(eventBus, times(1)).publish(captor.capture());

    assertThat(captor.getValue())
            .isInstanceOf(ShiftHandoverCompletedEvent.class)
            .asInstanceOf(type(ShiftHandoverCompletedEvent.class))
            .satisfies(e -> assertThat(e.updatedTaskIds()).containsExactly("a-1041", "a-1043"));
}
```

`times(1)` matters. A loop publishing one event per item passes a `verify(...).publish(any())` and
fails this. The listener's behaviour is tested separately, in `alerts`, with the event constructed
directly.

## Coverage thresholds

JaCoCo `check` goal, enforced per package in `pom.xml`. These are the gate; they are not targets to
aim just above.

| Scope | Line coverage |
|---|---|
| `**/service/**` | 80% |
| `**/web/**` | 70% |
| `shared/**` | 60% |
| Overall | 70% |

Coverage is necessary and not sufficient. A test that calls every branch and asserts only
`isNotNull()` satisfies JaCoCo and fails review under the Test Quality dimension. Never write a test
whose purpose is coverage — if a branch is hard to reach, that usually means the branch is dead, and
the fix is to delete it.

Do not lower a threshold. Do not add `@Generated`, JaCoCo exclusions, or `@ExcludeFromCoverage` to
pass. Any of those is an automatic FAIL on gate BUILD-2.

## Fixtures

Static factory methods on a per-module `*Fixtures` class in the test tree. No shared fixture class
across modules, no JSON files, no `@Sql`.

```java
static Task taskFixture(String id, TaskStatus status) {
    return new Task(id, "Restock aisle 4", null, status, TaskPriority.HIGH,
            TaskCategory.RESTOCKING, "prog-1", "user-7", "store-12",
            Instant.parse("2026-09-20T08:00:00Z"), Instant.now(), Instant.now());
}
```

Fixtures take the fields the test varies as parameters and default everything else. A test that has to
read a 12-argument constructor to understand what it varies is a test nobody maintains.

Use fixed `Instant` values, never `Instant.now()` for a field the assertion depends on. Due-date and
SLA tests must inject a `Clock` rather than reading the system clock — StoreOps runs across regions
and a test that passes only in one timezone is a flake that will be blamed on the harness.

## Running

```
./mvnw -q clean verify -Pharness-gates
```

Runs compile, Checkstyle, SpotBugs, all tests, ArchUnit, and the JaCoCo check. The Generator runs this
before writing `generator-summary.md`, and the Evaluator runs the same command. Same profile, same
thresholds, same config — a divergence between the two is a harness defect, not a code defect.
