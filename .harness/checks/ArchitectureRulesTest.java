package com.cognizant.storeops.architecture;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Hard gates ARCH-1 through ARCH-4 and the ArchUnit half of ERR-1.
 *
 * <p>Rule method names are referenced by ID in {@code .harness/skills/evaluation-criteria/SKILL.md}
 * and quoted verbatim in evaluator feedback. Renaming a rule breaks the audit trail — if a rule needs
 * to change, change its body and keep its name.
 *
 * <p>Do not modify this class to make a build pass. That is gate BUILD-2 and an automatic FAIL.
 * If a rule is genuinely wrong, it is wrong for every sprint: raise it with the engineering standards
 * team and change it in the open.
 *
 * <p>{@code DoNotIncludeTests} is load-bearing, not tidiness. Without it ArchUnit imports the test
 * tree, and test classes sit in {@code ..service..} packages while legitimately constructing
 * {@code IllegalArgumentException} in fixtures and asserting on {@code RuntimeException} — so ERR-1
 * and the layer rules would fail on test code that is doing nothing wrong. Removing it makes five
 * gates fire spuriously.
 *
 * <p>Placement: copy to {@code src/test/java/com/cognizant/storeops/architecture/}. It lives in
 * {@code .harness/checks/} in this repository so the harness's own gate definitions are reviewable
 * in one place alongside the Checkstyle config they pair with.
 */
@AnalyzeClasses(
        packages = "com.cognizant.storeops",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    static final String BASE = "com.cognizant.storeops";

    private static final List<String> MODULES =
            List.of("activities", "programmes", "staff", "alerts", "reports");

    // ---------------------------------------------------------------- ARCH-1

    /**
     * ARCH-1 — a repository is module-private. Catches client failure mode 1: direct imports from
     * another module's repository, bypassing the service boundary and the event bus.
     */
    @ArchTest
    static void repositoriesAreModulePrivate(JavaClasses classes) {
        for (String module : MODULES) {
            noClasses()
                    .that().resideOutsideOfPackage(BASE + "." + module + "..")
                    .should().dependOnClassesThat()
                    .resideInAPackage(BASE + "." + module + ".repository..")
                    .because("ARCH-1: " + module + ".repository is module-private; "
                            + "cross-module reads go through " + module + ".service")
                    .check(classes);
        }
    }

    // ---------------------------------------------------------------- ARCH-2

    /**
     * ARCH-2 — alerts is reached by event only. Catches failure mode 4: state changes written
     * directly into a sibling module instead of raised via the event bus.
     *
     * <p>The listener lives in alerts, so alerts depending on its own service is legal and not
     * covered here.
     */
    @ArchTest
    static final ArchRule alertsIsReachedByEventOnly =
            noClasses()
                    .that().resideOutsideOfPackage(BASE + ".alerts..")
                    .should().dependOnClassesThat().resideInAPackage(BASE + ".alerts.service..")
                    .because("ARCH-2: publish a StoreOpsEvent; do not call NotificationService");

    /** ARCH-2 — the same constraint for reports, which is only ever triggered by event. */
    @ArchTest
    static final ArchRule reportsIsReachedByEventOnly =
            noClasses()
                    .that().resideOutsideOfPackage(BASE + ".reports..")
                    .should().dependOnClassesThat().resideInAPackage(BASE + ".reports.service..")
                    .because("ARCH-2: publish a StoreOpsEvent; do not call ReportService");

    /**
     * ARCH-2 — the event bus must be reached through the StoreOpsEventBus interface. A direct
     * ApplicationEventPublisher injection is a legal Spring idiom that routes around the type the
     * two rules above inspect, so it has to be closed explicitly.
     */
    @ArchTest
    static final ArchRule eventBusIsUsedThroughTheInterface =
            noClasses()
                    .that().resideOutsideOfPackage(BASE + ".shared.event..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.context.ApplicationEventPublisher")
                    .because("ARCH-2: inject StoreOpsEventBus, not ApplicationEventPublisher");

    // ---------------------------------------------------------------- ERR-1

    /**
     * ERR-1 — no raw exception construction in a service or web class. Catches failure mode 2:
     * raw throws bypassing the {@code AppException} hierarchy, which return a 500 where the
     * contract promises a typed code.
     *
     * <p>This is the backstop. The primary ERR-1 gate is the Checkstyle {@code RegexpSinglelineJava}
     * rule in {@code checkstyle-harness.xml}, which catches any {@code throw new} regardless of
     * constructor signature. ArchUnit's {@code callConstructor} needs an exact parameter list, so the
     * common signatures are enumerated here; the two gates together close both the multi-line throw
     * Checkstyle's line regex misses and the unusual signature this rule misses.
     */
    @ArchTest
    static final ArchRule servicesThrowOnlyAppExceptions =
            noClasses()
                    .that().resideInAnyPackage(BASE + "..service..", BASE + "..web..")
                    .should().callConstructor(RuntimeException.class)
                    .orShould().callConstructor(RuntimeException.class, String.class)
                    .orShould().callConstructor(RuntimeException.class, String.class, Throwable.class)
                    .orShould().callConstructor(IllegalStateException.class)
                    .orShould().callConstructor(IllegalStateException.class, String.class)
                    .orShould().callConstructor(IllegalArgumentException.class)
                    .orShould().callConstructor(IllegalArgumentException.class, String.class)
                    .orShould().callConstructor(UnsupportedOperationException.class)
                    .orShould().callConstructor(UnsupportedOperationException.class, String.class)
                    .because("ERR-1: extend shared.error.AppException — code, message, httpStatus");

    // ---------------------------------------------------------------- ARCH-3

    /**
     * ARCH-3 — web to service to repository, one direction, no skipping.
     *
     * <p>This proves the dependency direction only. It cannot prove that business logic has not
     * leaked into a controller, because a rule check and a validation check are syntactically
     * identical. That half is LLM-assessed; see how-to-review.
     */
    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy(BASE + "..web..")
            .layer("Service").definedBy(BASE + "..service..")
            .layer("Repository").definedBy(BASE + "..repository..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Service")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .because("ARCH-3: routes hold HTTP concerns, services hold rules, "
                    + "repositories hold data access");

    /** ARCH-3 — a repository never reaches outward. No HTTP client, no event publishing. */
    @ArchTest
    static final ArchRule repositoriesDoNotReachOutward =
            noClasses()
                    .that().resideInAPackage(BASE + "..repository..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.http..",
                            BASE + ".shared.event..")
                    .because("ARCH-3: repositories do data access and nothing else");

    // ---------------------------------------------------------------- ARCH-4

    /**
     * ARCH-4 — reports aggregates, it never writes. Import-level half; the Evaluator reads every
     * reports diff for save/update/delete/publish calls on types reports does not own.
     */
    @ArchTest
    static final ArchRule reportsNeverWrite =
            noClasses()
                    .that().resideInAPackage(BASE + ".reports..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            BASE + ".activities.repository..",
                            BASE + ".programmes.repository..",
                            BASE + ".staff.repository..",
                            BASE + ".alerts.repository..")
                    .because("ARCH-4: reports reads through service interfaces and writes only "
                            + "to its own ReportRepository");

    // ---------------------------------------------------------------- staff

    /**
     * staff is read-only to every other module, per the ownership matrix in app-context. Not a
     * numbered gate — it is a corollary of ARCH-1 — but stated explicitly because a feature that
     * writes to staff is reshaping the domain, which is a developer decision rather than a harness
     * one.
     */
    @ArchTest
    static final ArchRule staffDependsOnNothing =
            noClasses()
                    .that().resideInAPackage(BASE + ".staff..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            BASE + ".activities..",
                            BASE + ".programmes..",
                            BASE + ".alerts..",
                            BASE + ".reports..")
                    .because("staff sits at the bottom of the dependency graph");
}
