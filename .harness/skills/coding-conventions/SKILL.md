# coding-conventions

**Purpose.** How StoreOps Java code is laid out and written. Read by the Generator only — the
Evaluator deliberately does not read this file, so nothing here is a gate. Violating a convention
costs points under the Build Integrity dimension via Checkstyle; violating an architecture rule from
`architecture-principles` fails a gate. Know which is which.

Stack: Java 17, Spring Boot 3.x, Maven, constructor injection, no Lombok.

## Package layout

```
com.cognizant.storeops
├── StoreOpsApplication.java
├── shared
│   ├── error          AppException + subclasses, GlobalExceptionHandler, ErrorResponse
│   ├── event          StoreOpsEventBus, event records
│   └── domain         cross-cutting value types (StoreId, RegionId)
└── <module>           activities | programmes | staff | alerts | reports
    ├── web            controllers, request/response DTOs
    ├── service        interface + Impl, business logic
    ├── repository     interface + in-memory Impl
    └── domain         entities, enums
```

`domain` holds no annotations from Spring or Jackson. Entities are not DTOs. Mapping between them
happens in `web` for inbound and outbound shapes, and nowhere else.

Tests mirror this exactly under `src/test/java`, plus `architecture/ArchitectureRulesTest.java` at the
`com.cognizant.storeops` root.

## Naming

| Thing | Pattern | Example |
|---|---|---|
| Controller | `<Aggregate>Controller` | `ActivityController` |
| Service interface | `<Aggregate>Service` | `ActivityService` |
| Service impl | `<Aggregate>ServiceImpl` | `ActivityServiceImpl` |
| Repository | `<Aggregate>Repository` / `InMemory<Aggregate>Repository` | `InMemoryTaskRepository` |
| Request DTO | `<Action><Aggregate>Request` | `BulkStatusUpdateRequest` |
| Response DTO | `<Action><Aggregate>Response` | `BulkStatusUpdateResponse` |
| Event | `<Aggregate><PastTenseVerb>Event` | `ShiftHandoverCompletedEvent` |
| Exception | `<Condition>Exception` | `InvalidTransitionException` |

Entities keep the names in `app-context` even where they read oddly against the retail domain —
`Task` not `Activity`, `Project` not `Programme`. The module is named for the domain; the type is
named for the existing code. Do not rename existing types as part of a feature; that is a refactor
and needs its own spec.

## Services

Interface plus impl, always. The interface is what other modules see, and ARCH-1 depends on there
being something to depend on that is not an implementation.

```java
public interface ActivityService {
    Task findById(String id);
    BulkStatusResult bulkUpdateStatus(BulkStatusCommand command);
}
```

- Constructor injection only. No `@Autowired` on fields, no setter injection.
- `final` fields.
- No `@Transactional` — storage is in-memory. If a sprint contract asks for transactional semantics,
  implement them explicitly in the service and say so in `Known gaps`.
- Services take commands and return domain types or purpose-built result records. Never accept or
  return a `web` DTO, and never return `ResponseEntity`.
- A service method that can partially succeed returns a result record carrying both outcomes. Never
  signal partial failure by throwing.

```java
public record BulkStatusResult(
        List<UpdatedItem> succeeded,
        List<FailedItem> failed) {

    public record UpdatedItem(String taskId, TaskStatus previousStatus, TaskStatus newStatus) {}
    public record FailedItem(String taskId, String reason) {}
}
```

Records for anything immutable — results, commands, events, DTOs. Classes only for entities that
mutate and for exceptions.

## Errors

Never `throw new RuntimeException`. Ever, anywhere in `service` or `web`. This is gate ERR-1 and it
fails the build.

```java
public abstract class AppException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    protected AppException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    public String code() { return code; }
    public HttpStatus httpStatus() { return httpStatus; }
}
```

Existing subclasses — use these before writing a new one:

| Subclass | Status | Use for |
|---|---|---|
| `ValidationException` | 400 | Malformed input that Bean Validation did not catch |
| `UnauthorizedException` | 401 | Missing or expired `AuthToken` |
| `ForbiddenException` | 403 | Valid user, insufficient `StaffRole` |
| `NotFoundException` | 404 | Entity id does not resolve |
| `ConflictException` | 409 | Concurrent modification, duplicate creation |
| `RuleViolationException` | 422 | A business rule refused the operation |

Codes are `SCREAMING_SNAKE`, stable, and part of the API contract. Clients branch on them, so never
change one to fix a test. New codes need a line in the sprint contract.

```java
throw new RuleViolationException(
    "INVALID_TRANSITION",
    "Activity %s cannot move from %s to %s".formatted(id, from, to));
```

A partial-failure `reason` string uses the same code vocabulary as an exception would —
`INVALID_TRANSITION`, `NOT_FOUND`, `FORBIDDEN` — so a client handles one item's failure the same way
it handles a whole-request failure.

## Controllers

```java
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<BulkStatusUpdateResponse> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            @AuthenticationPrincipal StoreOpsUser user) {

        BulkStatusResult result = activityService.bulkUpdateStatus(request.toCommand(user.id()));
        HttpStatus status = result.failed().isEmpty() ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(BulkStatusUpdateResponse.from(result));
    }
}
```

What that example is demonstrating, because each line is a convention:

- Constructor injection, one service dependency.
- `@Valid` on the request — shape validation is declarative and lives on the DTO, not in the method.
- DTO → command conversion is a method on the DTO, so the service never sees a web type.
- The status decision is HTTP mapping, which is legitimately a controller concern. Deciding *which
  items* failed is not, and lives in the service. That line is the boundary ARCH-3 is about.
- No try/catch. `GlobalExceptionHandler` owns error mapping.

## Validation

Bean Validation on DTO fields for shape. Business rules in the service. The test: could this
constraint be checked without loading any data? If yes, DTO. If it needs to look at an entity's
current state, service.

```java
public record BulkStatusUpdateRequest(
        @NotEmpty @Size(max = 200) List<@Valid ItemUpdate> updates) {

    public record ItemUpdate(
            @NotBlank String taskId,
            @NotNull TaskStatus targetStatus,
            @Size(max = 500) String note) {}
}
```

"Target status must be `DONE` or `BLOCKED`" is a business rule about handover semantics, not a shape
constraint — service layer, `RuleViolationException`, code `UNSUPPORTED_TARGET_STATUS`.

## Events

```java
public interface StoreOpsEventBus {
    void publish(StoreOpsEvent event);
}
```

Events are records implementing `StoreOpsEvent`, live in `shared.event`, and are named in past tense
because they describe something that has already happened. Publish after the state change, never
before, and never inside a loop over items — one event per operation, not one per item, unless the
contract says otherwise.

Full usage is in `api-integration`.

## Style specifics that Checkstyle enforces

- 4-space indent, no tabs. 120-character lines.
- One top-level type per file.
- Import order: `java`, `javax`, third-party, `com.cognizant`. No wildcard imports, no unused imports.
- **Javadoc on every public method and every public constructor, except where `@Override` is
  present.** Checkstyle's `JavadocMethod` runs with `accessModifiers=public` and its default
  `allowedAnnotations=Override`, so an impl method carrying `@Override` is exempt and a controller
  handler, an interface method, a static factory on a DTO and a public constructor are not. This is
  gate `BUILD-1`, not a preference: a controller written without Javadoc fails the build.
  `@param` and `@return` tags are optional (`allowMissingParamTags` and `allowMissingReturnTag` are
  both on), so a one-line summary is enough. A Javadoc block that restates the method name is noise;
  Checkstyle will not catch that but a reviewer will.
- No `System.out`. `SLF4J` logger, `private static final Logger log = LoggerFactory.getLogger(X.class)`.
- Never log a `payload`, token, or email at `INFO`. Store staff PII sits in these objects.
- No field shadowing, no `!isEmpty()` where `isEmpty()` would read better, no nested ternaries.

## Things not to do

- Do not add a Maven dependency. If a sprint needs one, stop and put it in `Known gaps`.
- Do not introduce Lombok, MapStruct, or a new mapping library.
- Do not add `@SuppressWarnings`, `@Disabled`, or `checkstyle:off` comments to make a gate pass.
- Do not reformat a file you did not otherwise change. It buries the real diff and makes the
  Evaluator's line references useless.
- Do not add a `utils` or `helper` package. Behaviour belongs to a type.
