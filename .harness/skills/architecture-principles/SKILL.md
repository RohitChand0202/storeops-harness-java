# architecture-principles

**Purpose.** The five non-negotiable StoreOps architecture rules, each with a gate ID, the exact tool
that proves it, and what breaks if it is removed. Read by the Planner, Generator, and Evaluator.

Gate IDs are stable. They appear in `sprint-N-contract.md` architecture notes, in
`evaluator-feedback.md` under `HARD_GATES_FAILED`, and in escalation notices. Never renumber them.

---

## ARCH-1 — Module boundary

**Rule.** No class in one module may reference any class in another module's `repository` package.
Cross-module reads go through the target module's `*Service` interface, where the ownership matrix in
`app-context` permits a read at all — `alerts` and `reports` are event-only and admit no read either
(see ARCH-2). Cross-module writes do not happen at all (see ARCH-2).

**Proof.** ArchUnit, `ArchitectureRulesTest#repositoriesAreModulePrivate`. Deterministic: the rule
either finds a violating class reference or it does not.

```java
@ArchTest
static void repositoriesAreModulePrivate(JavaClasses classes) {
    for (String module : MODULES) {          // activities, programmes, staff, alerts, reports
        noClasses()
                .that().resideOutsideOfPackage(BASE + "." + module + "..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + "." + module + ".repository..")
                .because("ARCH-1: " + module + ".repository is module-private")
                .check(classes);
    }
}
```

One rule per module rather than a single global pattern, because the failure message then names
*which* module's repository was reached and from where. `ARCH-1: alerts.repository is module-private`
is quotable in evaluator feedback; a generic slice violation is not.

**Why it cannot be a soft check.** A boundary violation compiles, passes tests, and produces correct
behaviour on the day it is written. It only shows up later, when the sibling module changes its
storage shape and an unrelated module breaks. There is no test the Generator could plausibly write
that catches it, so the only place it can be caught is a dependency analyser. This is failure mode 1
and it is the reason the standards team asked for a harness.

**What breaks without it.** The five modules stop being five modules. `TaskRepository` becomes a
shared data layer, `reports` starts reading task rows directly for speed, and within two sprints no
module can change its persistence without a cross-team negotiation.

---

## ARCH-2 — Event bus only for cross-module side effects

**Rule.** Any state change whose effect lands in another module is raised as an event via
`StoreOpsEventBus.publish(...)`. A module never injects, imports, or calls another module's service
to *cause* a change.

Reads are exempt — with two named exceptions that the gate enforces and that you must know before
you write a cross-module call. **`alerts` and `reports` are event-only in both directions.** No class
outside `alerts` may depend on `alerts.service` at all, and no class outside `reports` may depend on
`reports.service` at all, read or write. `activities` reading `programmes.service` for a lookup is
legal; `activities` reading `alerts.service` to check whether a notification already exists is not,
and the ArchUnit rule below fails it. The ownership matrix in `app-context` is the authority: "read
via service" means reads are permitted, "event only" and "never" mean no dependency of any kind.

The reason is not symmetry. Those two modules are pure consumers — `alerts` exists to react and
`reports` exists to aggregate — so a read *into* either one is a module reaching for a side effect's
result, which is the shape a direct write takes once someone needs to confirm it happened.

Concretely, and these are the cases that come up: a `CRITICAL` task going overdue raises
`SlaBreachedEvent`, it does not call `NotificationService.create(...)`. A programme closing raises
`ProgrammeClosedEvent`, it does not call `ReportService.generate(...)`. A shift handover raises
`ShiftHandoverCompletedEvent`.

**Proof.** ArchUnit, `ArchitectureRulesTest#alertsIsReachedByEventOnly` — no class in `activities`,
`programmes`, `reports`, or `staff` may depend on `alerts.service` or `reports.service` at all. Plus
LLM-assessed review of the diff for any new cross-module service injection that ArchUnit's package
rules do not cover (e.g. reflection, a shared interface used as an indirection).

```java
@ArchTest
static final ArchRule alertsIsReachedByEventOnly =
    noClasses()
        .that().resideOutsideOfPackage(BASE + ".alerts..")
        .should().dependOnClassesThat().resideInAPackage(BASE + ".alerts.service..")
        .because("ARCH-2: publish a StoreOpsEvent; do not call NotificationService");
```

A matching rule covers `reports.service`. A third rule closes the route around both: inject
`StoreOpsEventBus`, never Spring's `ApplicationEventPublisher` directly, because a direct publisher
injection is a legal Spring idiom that bypasses the type the two rules above inspect.

```java
@ArchTest
static final ArchRule eventBusIsUsedThroughTheInterface =
    noClasses()
        .that().resideOutsideOfPackage(BASE + ".shared.event..")
        .should().dependOnClassesThat()
        .haveFullyQualifiedName("org.springframework.context.ApplicationEventPublisher")
        .because("ARCH-2: inject StoreOpsEventBus, not ApplicationEventPublisher");
```

**Why it cannot be a soft check.** A direct call is the shorter, more obvious code. Every LLM
generating "notify the department lead" will reach for `NotificationService` first, because that is
what the training distribution contains. Left as advisory, it gets written every time.

**What breaks without it.** Side effects become invisible. Nothing can enumerate what happens when a
task goes overdue, because the answer is distributed across whichever services happened to import
each other. This is failure mode 4.

---

## ERR-1 — Typed error contract

**Rule.** No `service` or `web` package class may throw `RuntimeException`, `IllegalStateException`,
`IllegalArgumentException`, `UnsupportedOperationException`, or bare `Exception`. All errors extend
`AppException`, which carries `code` (stable string), `message` (human-readable), and `httpStatus`.

Subclasses and their status: `ValidationException` 400, `UnauthorizedException` 401,
`ForbiddenException` 403, `NotFoundException` 404, `ConflictException` 409, `RuleViolationException`
422. `GlobalExceptionHandler` in `shared` maps `AppException` to the response body; it maps anything
else to a 500 with code `INTERNAL_ERROR` and no stack trace.

**Proof.** Two gates, both automated. Checkstyle `RegexpSinglelineJava` (see
`.harness/checks/checkstyle-harness.xml`) fails the build on a matching `throw new` in a service or
web path. ArchUnit `ArchitectureRulesTest#servicesThrowOnlyAppExceptions` catches constructor calls
Checkstyle's line regex misses, such as a multi-line throw or one wrapped in a helper.

```java
@ArchTest
static final ArchRule servicesThrowOnlyAppExceptions =
    noClasses()
        .that().resideInAnyPackage(BASE + "..service..", BASE + "..web..")
        .should().callConstructor(RuntimeException.class)
        .orShould().callConstructor(RuntimeException.class, String.class)
        .orShould().callConstructor(IllegalStateException.class, String.class)
        .orShould().callConstructor(IllegalArgumentException.class, String.class)
        // remaining signatures enumerated in ArchitectureRulesTest
        .because("ERR-1: errors are typed, see shared.error.AppException");
```

The two gates are complementary and both are needed. `callConstructor` requires an exact parameter
list, so ArchUnit misses an unusual signature; the Checkstyle regex is line-based, so it misses a
throw split across lines. Between them there is no gap. Neither catches a `catch (Exception e)` that
absorbs a failure and returns a default — Checkstyle's `IllegalCatch` flags the catch, and the
Evaluator reads the block.

**Why it cannot be a soft check.** The failure is silent and user-facing. A raw throw produces a 500
where the contract promises a 422, and no test the Generator writes will notice, because the
Generator writing the throw is the same one writing the test's expected status.

**What breaks without it.** Clients cannot branch on error. The handheld app that store staff use has
no way to distinguish "this activity is already DONE" from "the server fell over", so it shows the
same unhelpful message for both. This is failure mode 2.

---

## ARCH-3 — Layer separation

**Rule.** `web` → `service` → `repository`, one direction, no skipping.

- `web` (controllers): HTTP concerns only — routing, DTO binding, Bean Validation, mapping
  `AppException` to a response. No business rule may live here. A controller that decides whether a
  status transition is legal is a violation even if the decision is correct.
- `service`: all business logic. Knows nothing about `HttpServletRequest`, `ResponseEntity`, or
  status codes. Returns domain types and throws `AppException` subclasses.
- `repository`: data access only. No calls to other services, no HTTP clients, no event publishing.

**Proof.** ArchUnit layered architecture rule, plus an Evaluator check that the `Files changed` layer
column in `generator-summary.md` matches each file's actual package.

```java
@ArchTest
static final ArchRule layersAreRespected = layeredArchitecture().consideringOnlyDependenciesInLayers()
    .layer("Web").definedBy(BASE + "..web..")
    .layer("Service").definedBy(BASE + "..service..")
    .layer("Repository").definedBy(BASE + "..repository..")
    .whereLayer("Web").mayNotBeAccessedByAnyLayer()
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Service")
    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
    .because("ARCH-3: web → service → repository, no skipping");
```

**Why it is partly LLM-assessed.** ArchUnit proves the dependency direction. It cannot prove that
business logic has not leaked into a controller, because a rule check in a controller is
syntactically identical to a validation check. That half needs a reviewer reading the diff.

**What breaks without it.** Logic duplicates. The transition rule lives in the controller for one
endpoint and the service for another, they drift, and the same activity behaves differently depending
on which route touched it.

---

## ARCH-4 — Reports are read-only

**Rule.** No class in `com.cognizant.storeops.reports` may invoke a write on any other module —
directly, through a service, or by publishing an event that causes a write elsewhere. `reports`
writes to its own `ReportRepository` and nowhere else. It may *consume* events; it may not cause
side effects with them.

**Proof.** ArchUnit dependency rule for the import-level case, plus LLM-assessed review of every
`reports` diff for `save`, `update`, `delete`, or `publish` calls on non-`reports` types.

```java
@ArchTest
static final ArchRule reportsNeverWrite =
    noClasses()
        .that().resideInAPackage("com.cognizant.storeops.reports..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "com.cognizant.storeops.activities.repository..",
            "com.cognizant.storeops.programmes.repository..",
            "com.cognizant.storeops.staff.repository..",
            "com.cognizant.storeops.alerts.repository..")
        .because("ARCH-4: reports aggregates, it never writes");
```

**Why it cannot be a soft check.** A reporting module that writes is how aggregate tables get
silently mutated. The damage is to data, and by the time it surfaces the audit trail of who changed
what is already gone.

**What breaks without it.** Reports stop being reproducible. A report run changes the data the next
report run reads, and two identical requests return different numbers.

---

## Precedence

When rules appear to conflict, this is the order:

1. ERR-1 — a typed error is never the wrong choice
2. ARCH-1 — never cross a repository boundary, whatever the reason
3. ARCH-2 — prefer an event to a call
4. ARCH-4 — `reports` gives way to everything
5. ARCH-3 — layering yields last, and only for framework-mandated exceptions
   (e.g. a Spring `@ControllerAdvice` in `shared` touching web types), which must be named in
   `Known gaps`

A rule that seems to require violating another rule usually means the feature was decomposed wrongly.
Escalate rather than choosing.
