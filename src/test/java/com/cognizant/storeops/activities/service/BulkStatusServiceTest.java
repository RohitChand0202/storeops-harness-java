package com.cognizant.storeops.activities.service;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.AuditEntry;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.repository.AuditEntryRepository;
import com.cognizant.storeops.activities.repository.InMemoryAuditEntryRepository;
import com.cognizant.storeops.activities.repository.InMemoryTaskRepository;
import com.cognizant.storeops.activities.repository.TaskRepository;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.ForbiddenException;
import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.shared.event.ShiftHandoverCompletedEvent;
import com.cognizant.storeops.shared.event.StoreOpsEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkStatusServiceTest {

    private static final StoreOpsUser ACTOR =
            new StoreOpsUser("user-7", "store-12", "region-1", "ASSOCIATE");

    private InMemoryTaskRepository taskRepository;
    private InMemoryAuditEntryRepository auditRepository;
    private StoreOpsEventBus eventBus;
    private ActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        auditRepository = new InMemoryAuditEntryRepository();
        eventBus = mock(StoreOpsEventBus.class);
        service = new ActivityServiceImpl(taskRepository, auditRepository, eventBus,
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));
    }

    @Test
    void appliesEveryValidTransitionAndAuditsEachOnce() {
        taskRepository.save(ActivityFixtures.task("a-1", TaskStatus.TODO));
        taskRepository.save(ActivityFixtures.task("a-2", TaskStatus.IN_PROGRESS));
        taskRepository.save(ActivityFixtures.task("a-3", TaskStatus.IN_PROGRESS));

        BulkStatusResult result = service.bulkUpdateStatus(command(
                item("a-1", TaskStatus.DONE, "done at handover"),
                item("a-2", TaskStatus.BLOCKED, "shelf unavailable"),
                item("a-3", TaskStatus.DONE, null)));

        assertThat(result.succeeded()).extracting(BulkStatusResult.UpdatedItem::taskId)
                .containsExactly("a-1", "a-2", "a-3");
        assertThat(result.succeeded()).containsExactly(
                new BulkStatusResult.UpdatedItem("a-1", TaskStatus.TODO, TaskStatus.DONE),
                new BulkStatusResult.UpdatedItem("a-2", TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED),
                new BulkStatusResult.UpdatedItem("a-3", TaskStatus.IN_PROGRESS, TaskStatus.DONE));
        assertThat(result.failed()).isEmpty();
        assertThat(taskRepository.findById("a-1").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(taskRepository.findById("a-2").orElseThrow().getStatus()).isEqualTo(TaskStatus.BLOCKED);
        assertThat(taskRepository.findById("a-3").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);

        List<AuditEntry> firstAudit = auditRepository.findByTaskId("a-1");
        assertThat(firstAudit).hasSize(1);
        assertThat(firstAudit.get(0).previousStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(firstAudit.get(0).newStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(firstAudit.get(0).actingUserId()).isEqualTo("user-7");
        assertThat(firstAudit.get(0).note()).isEqualTo("done at handover");
        assertThat(firstAudit.get(0).occurredAt()).isEqualTo(ActivityFixtures.AT);
        assertThat(auditRepository.findByTaskId("a-2")).hasSize(1);
        assertThat(auditRepository.findByTaskId("a-3")).hasSize(1);

        ArgumentCaptor<StoreOpsEvent> eventCaptor = ArgumentCaptor.forClass(StoreOpsEvent.class);
        verify(eventBus, times(1)).publish(eventCaptor.capture());
        ShiftHandoverCompletedEvent event = (ShiftHandoverCompletedEvent) eventCaptor.getValue();
        assertThat(event.storeId()).isEqualTo("store-12");
        assertThat(event.handoverByUserId()).isEqualTo("user-7");
        assertThat(event.updatedTaskIds()).containsExactly("a-1", "a-2", "a-3");
        assertThat(event.occurredAt()).isEqualTo(ActivityFixtures.AT);
    }

    @Test
    void reportsIndependentFailuresWithoutChangingOrAuditingThoseItems() {
        taskRepository.save(ActivityFixtures.task("done", TaskStatus.DONE));
        taskRepository.save(ActivityFixtures.task("valid", TaskStatus.TODO));
        Task foreign = new Task("foreign", "Foreign task", "store-99", TaskStatus.TODO,
                TaskPriority.HIGH, TaskCategory.RESTOCKING, ActivityFixtures.AT);
        taskRepository.save(foreign);

        BulkStatusResult result = service.bulkUpdateStatus(command(
                item("missing", TaskStatus.DONE, null),
                item("done", TaskStatus.BLOCKED, null),
                item("foreign", TaskStatus.DONE, null),
                item("valid", TaskStatus.DONE, null),
                item("done", TaskStatus.DONE, null),
                item("valid", TaskStatus.TODO, null)));

        assertThat(result.succeeded()).extracting(BulkStatusResult.UpdatedItem::taskId)
                .containsExactly("valid");
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("missing", "NOT_FOUND"),
                new BulkStatusResult.FailedItem("done", "INVALID_TRANSITION"),
                new BulkStatusResult.FailedItem("foreign", "FORBIDDEN"),
                new BulkStatusResult.FailedItem("done", "ALREADY_IN_STATUS"),
                new BulkStatusResult.FailedItem("valid", "UNSUPPORTED_TARGET_STATUS"));
        assertThat(taskRepository.findById("done").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(taskRepository.findById("foreign").orElseThrow().getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(taskRepository.findById("valid").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(auditRepository.findByTaskId("done")).isEmpty();
        assertThat(auditRepository.findByTaskId("foreign")).isEmpty();
        assertThat(auditRepository.findByTaskId("missing")).isEmpty();
        assertThat(auditRepository.findByTaskId("valid")).hasSize(1);
        ArgumentCaptor<StoreOpsEvent> eventCaptor = ArgumentCaptor.forClass(StoreOpsEvent.class);
        verify(eventBus, times(1)).publish(eventCaptor.capture());
        ShiftHandoverCompletedEvent event = (ShiftHandoverCompletedEvent) eventCaptor.getValue();
        assertThat(event.storeId()).isEqualTo("store-12");
        assertThat(event.handoverByUserId()).isEqualTo("user-7");
        assertThat(event.updatedTaskIds()).containsExactly("valid");
        assertThat(event.occurredAt()).isEqualTo(ActivityFixtures.AT);
    }

    @Test
    void repeatedItemDoesNotCreateASecondAuditEntryOrSuccess() {
        taskRepository.save(ActivityFixtures.task("a-1", TaskStatus.TODO));

        BulkStatusResult result = service.bulkUpdateStatus(command(
                item("a-1", TaskStatus.DONE, null), item("a-1", TaskStatus.DONE, null)));

        assertThat(result.succeeded()).containsExactly(
                new BulkStatusResult.UpdatedItem("a-1", TaskStatus.TODO, TaskStatus.DONE));
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("a-1", "ALREADY_IN_STATUS"));
        assertThat(taskRepository.findById("a-1").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(auditRepository.findByTaskId("a-1")).hasSize(1);
    }

    @Test
    void rejectsBothUnsupportedHandoverTargetsWithoutWrites() {
        Task todo = ActivityFixtures.task("todo", TaskStatus.TODO);
        Task inProgress = ActivityFixtures.task("in-progress", TaskStatus.IN_PROGRESS);
        taskRepository.save(todo);
        taskRepository.save(inProgress);

        BulkStatusResult result = service.bulkUpdateStatus(command(
                item("todo", TaskStatus.IN_PROGRESS, null),
                item("in-progress", TaskStatus.TODO, null)));

        assertThat(result.succeeded()).isEmpty();
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("todo", "UNSUPPORTED_TARGET_STATUS"),
                new BulkStatusResult.FailedItem("in-progress", "UNSUPPORTED_TARGET_STATUS"));
        assertThat(todo.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(inProgress.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(auditRepository.findByTaskId("todo")).isEmpty();
        assertThat(auditRepository.findByTaskId("in-progress")).isEmpty();
        verify(eventBus, never()).publish(any());
    }

    @Test
    void allRejectedItemsCauseNoAuditAndNoEvent() {
        taskRepository.save(ActivityFixtures.task("done", TaskStatus.DONE));

        BulkStatusResult result = service.bulkUpdateStatus(command(
                item("done", TaskStatus.BLOCKED, null),
                item("missing", TaskStatus.DONE, null)));

        assertThat(result.succeeded()).isEmpty();
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("done", "INVALID_TRANSITION"),
                new BulkStatusResult.FailedItem("missing", "NOT_FOUND"));
        assertThat(auditRepository.findByTaskId("done")).isEmpty();
        verify(eventBus, never()).publish(any());
    }

    @Test
    void rejectsEveryInvalidItemWithoutWritingToEitherRepository() {
        TaskRepository tasks = mock(TaskRepository.class);
        AuditEntryRepository audits = mock(AuditEntryRepository.class);
        Task alreadyDone = ActivityFixtures.task("done", TaskStatus.DONE);
        when(tasks.findAllById(List.of("done"))).thenReturn(List.of(alreadyDone));
        ActivityServiceImpl isolated = new ActivityServiceImpl(tasks, audits, eventBus,
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));

        BulkStatusResult result = isolated.bulkUpdateStatus(command(item("done", TaskStatus.BLOCKED, null)));

        assertThat(result.succeeded()).isEmpty();
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("done", "INVALID_TRANSITION"));
        verify(tasks, never()).save(any());
        verify(audits, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void orderedAllFailedBatchDoesNotSaveAnyTaskOrAudit() {
        TaskRepository tasks = mock(TaskRepository.class);
        AuditEntryRepository audits = mock(AuditEntryRepository.class);
        Task alreadyDone = ActivityFixtures.task("done", TaskStatus.DONE);
        when(tasks.findAllById(List.of("done", "missing", "done")))
                .thenReturn(List.of(alreadyDone));
        ActivityServiceImpl isolated = new ActivityServiceImpl(tasks, audits, eventBus,
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));

        BulkStatusResult result = isolated.bulkUpdateStatus(command(
                item("done", TaskStatus.BLOCKED, null),
                item("missing", TaskStatus.DONE, null),
                item("done", TaskStatus.DONE, null)));

        assertThat(result.succeeded()).isEmpty();
        assertThat(result.failed()).containsExactly(
                new BulkStatusResult.FailedItem("done", "INVALID_TRANSITION"),
                new BulkStatusResult.FailedItem("missing", "NOT_FOUND"),
                new BulkStatusResult.FailedItem("done", "ALREADY_IN_STATUS"));
        verify(tasks, never()).save(any());
        verify(audits, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void allForeignItemsFailTheWholeRequestWithoutChangingState() {
        Task foreign = new Task("foreign", "Foreign task", "store-99", TaskStatus.TODO,
                TaskPriority.HIGH, TaskCategory.RESTOCKING, ActivityFixtures.AT);
        Task anotherForeign = new Task("foreign-2", "Another foreign task", "store-98",
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TaskCategory.RESTOCKING,
                ActivityFixtures.AT);
        taskRepository.save(foreign);
        taskRepository.save(anotherForeign);

        ForbiddenException failure = assertThrows(ForbiddenException.class,
                () -> service.bulkUpdateStatus(command(
                        item("foreign", TaskStatus.DONE, null),
                        item("foreign-2", TaskStatus.BLOCKED, null))));
        assertThat(failure.code()).isEqualTo("FORBIDDEN");
        assertThat(foreign.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(anotherForeign.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(auditRepository.findByTaskId("foreign")).isEmpty();
        assertThat(auditRepository.findByTaskId("foreign-2")).isEmpty();
        verify(eventBus, never()).publish(any());
    }

    @Test
    void missingActorFailsBeforeAnyTransitionOrAudit() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        taskRepository.save(task);
        BulkStatusCommand unauthenticated = new BulkStatusCommand(null,
                List.of(item("a-1", TaskStatus.DONE, null)));

        UnauthorizedException failure = assertThrows(UnauthorizedException.class,
                () -> service.bulkUpdateStatus(unauthenticated));

        assertThat(failure.code()).isEqualTo("UNAUTHORIZED");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(auditRepository.findByTaskId("a-1")).isEmpty();
        verify(eventBus, never()).publish(any());
    }

    @Test
    void concurrentHandoversApplyAndAuditTheSameTransitionOnlyOnce() throws Exception {
        CoordinatedTask task = new CoordinatedTask("a-1");
        taskRepository.save(task);
        BulkStatusCommand request = command(item("a-1", TaskStatus.DONE, null));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<BulkStatusResult> first = workers.submit(() -> {
                start.await();
                return service.bulkUpdateStatus(request);
            });
            Future<BulkStatusResult> second = workers.submit(() -> {
                start.await();
                return service.bulkUpdateStatus(request);
            });
            start.countDown();
            BulkStatusResult firstResult = first.get(10, TimeUnit.SECONDS);
            BulkStatusResult secondResult = second.get(10, TimeUnit.SECONDS);
            task.stopCoordination();

            assertThat(firstResult.succeeded().size() + secondResult.succeeded().size()).isEqualTo(1);
            assertThat(firstResult.failed().size() + secondResult.failed().size()).isEqualTo(1);
            assertThat(List.of(firstResult, secondResult).stream()
                    .flatMap(result -> result.failed().stream())
                    .map(BulkStatusResult.FailedItem::reason))
                    .containsExactly("ALREADY_IN_STATUS");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(auditRepository.findByTaskId("a-1")).hasSize(1);
            verify(eventBus, times(1)).publish(any(ShiftHandoverCompletedEvent.class));
        } finally {
            task.stopCoordination();
            workers.shutdownNow();
        }
    }

    @Test
    void deleteBeforeAQueuedHandoverCannotResurrectTheActivity() throws Exception {
        InMemoryTaskRepository backing = new InMemoryTaskRepository();
        backing.save(ActivityFixtures.task("a-1", TaskStatus.TODO));
        TaskRepository pausedRepository = mock(TaskRepository.class);
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        when(pausedRepository.findAllById(List.of("a-1"))).thenAnswer(invocation -> {
            List<Task> snapshot = backing.findAllById(List.of("a-1"));
            loaded.countDown();
            resume.await(5, TimeUnit.SECONDS);
            return snapshot;
        });
        when(pausedRepository.findById("a-1")).thenAnswer(invocation -> backing.findById("a-1"));
        when(pausedRepository.deleteById("a-1")).thenAnswer(invocation -> backing.deleteById("a-1"));
        when(pausedRepository.save(any())).thenAnswer(invocation -> backing.save(invocation.getArgument(0)));
        ActivityServiceImpl isolated = new ActivityServiceImpl(pausedRepository, auditRepository, eventBus,
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<BulkStatusResult> pending = worker.submit(() -> isolated.bulkUpdateStatus(
                    command(item("a-1", TaskStatus.DONE, null))));
            assertThat(loaded.await(5, TimeUnit.SECONDS)).isTrue();
            isolated.delete(ACTOR, "a-1");
            resume.countDown();

            BulkStatusResult result = pending.get(5, TimeUnit.SECONDS);
            assertThat(result.succeeded()).isEmpty();
            assertThat(result.failed()).containsExactly(
                    new BulkStatusResult.FailedItem("a-1", "NOT_FOUND"));
            assertThat(backing.findById("a-1")).isEmpty();
            assertThat(auditRepository.findByTaskId("a-1")).isEmpty();
            verify(eventBus, never()).publish(any());
        } finally {
            resume.countDown();
            worker.shutdownNow();
        }
    }

    private BulkStatusCommand command(BulkStatusCommand.ItemUpdate... items) {
        return new BulkStatusCommand(ACTOR, List.of(items));
    }

    private BulkStatusCommand.ItemUpdate item(String id, TaskStatus status, String note) {
        return new BulkStatusCommand.ItemUpdate(id, status, note);
    }

    private static final class CoordinatedTask extends Task {

        private final CountDownLatch[] statusReads = {
            new CountDownLatch(2), new CountDownLatch(2), new CountDownLatch(2)
        };
        private final ThreadLocal<Integer> readNumber = ThreadLocal.withInitial(() -> 0);
        private volatile boolean coordinating = true;

        CoordinatedTask(String id) {
            super(id, "Concurrent task", "store-12", TaskStatus.TODO,
                    TaskPriority.HIGH, TaskCategory.RESTOCKING, ActivityFixtures.AT);
        }

        @Override
        public TaskStatus getStatus() {
            TaskStatus snapshot = super.getStatus();
            int read = readNumber.get();
            readNumber.set(read + 1);
            if (coordinating && read < statusReads.length) {
                statusReads[read].countDown();
                try {
                    statusReads[read].await(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return snapshot;
        }

        void stopCoordination() {
            coordinating = false;
        }
    }
}
