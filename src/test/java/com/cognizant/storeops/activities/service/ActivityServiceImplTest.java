package com.cognizant.storeops.activities.service;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.AuditEntry;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.repository.AuditEntryRepository;
import com.cognizant.storeops.activities.repository.TaskRepository;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.shared.event.StoreOpsEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;
import com.cognizant.storeops.shared.event.TaskBlockedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceImplTest {

    private TaskRepository taskRepository;
    private AuditEntryRepository auditEntryRepository;
    private StoreOpsEventBus eventBus;
    private ActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        auditEntryRepository = mock(AuditEntryRepository.class);
        eventBus = mock(StoreOpsEventBus.class);
        service = new ActivityServiceImpl(taskRepository, auditEntryRepository, eventBus,
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));
    }

    @Test
    void listsDelegatingBothFilters() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        when(taskRepository.findAll("prog-1", TaskStatus.TODO)).thenReturn(List.of(task));

        assertThat(service.list("prog-1", TaskStatus.TODO)).containsExactly(task);
    }

    @Test
    void findsById() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        when(taskRepository.findById("a-1")).thenReturn(Optional.of(task));

        assertThat(service.findById("a-1")).isSameAs(task);
    }

    @Test
    void throwsTypedNotFoundForUnknownId() {
        when(taskRepository.findById("a-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("a-9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("a-9999");
        assertThat(((NotFoundException) org.junit.jupiter.api.Assertions.assertThrows(
                NotFoundException.class, () -> service.findById("a-9999"))).code())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void createsActivityInTodoAndPersistsIt() {
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Task created = service.create(new CreateActivityCommand("Reset bay 3", "detail", "store-12",
                TaskPriority.MEDIUM, TaskCategory.PLANOGRAM, "prog-1", "user-7"));

        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.getTitle()).isEqualTo("Reset bay 3");
        assertThat(created.getProgrammeId()).isEqualTo("prog-1");
        assertThat(created.getAssigneeId()).isEqualTo("user-7");
        assertThat(created.getCreatedAt()).isEqualTo(ActivityFixtures.AT);
        verify(taskRepository, times(1)).save(created);
    }

    @Test
    void appliesTransitionAndWritesExactlyOneAuditEntry() {
        Task task = ActivityFixtures.task("a-1041", TaskStatus.IN_PROGRESS);
        when(taskRepository.findById("a-1041")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update("a-1041", new UpdateActivityCommand(TaskStatus.DONE, null, null, null, "user-7"));

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().taskId()).isEqualTo("a-1041");
        assertThat(captor.getValue().previousStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(captor.getValue().newStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(captor.getValue().actingUserId()).isEqualTo("user-7");
        verify(eventBus, never()).publish(any());
    }

    @Test
    void rejectsTransitionFromDoneAndChangesNothing() {
        Task task = ActivityFixtures.task("a-1042", TaskStatus.DONE);
        when(taskRepository.findById("a-1042")).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.update("a-1042",
                new UpdateActivityCommand(TaskStatus.BLOCKED, null, null, null, "user-7")))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("cannot move from DONE to BLOCKED");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository, never()).save(any());
        verify(auditEntryRepository, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void publishesExactlyOneTaskBlockedEventWhenBlocking() {
        Task task = ActivityFixtures.task("a-1043", TaskStatus.TODO);
        when(taskRepository.findById("a-1043")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update("a-1043", new UpdateActivityCommand(TaskStatus.BLOCKED, null, null, null, "user-9"));

        ArgumentCaptor<StoreOpsEvent> captor = ArgumentCaptor.forClass(StoreOpsEvent.class);
        verify(eventBus, times(1)).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TaskBlockedEvent.class);
        TaskBlockedEvent event = (TaskBlockedEvent) captor.getValue();
        assertThat(event.taskId()).isEqualTo("a-1043");
        assertThat(event.blockedByUserId()).isEqualTo("user-9");
        assertThat(event.occurredAt()).isEqualTo(ActivityFixtures.AT);
        assertThat(event.eventType()).isEqualTo("TASK_BLOCKED");
    }

    @Test
    void updatesNonStatusFieldsWithoutAuditOrEvent() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        when(taskRepository.findById("a-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update("a-1", new UpdateActivityCommand(null, TaskPriority.CRITICAL,
                TaskCategory.COMPLIANCE, "user-3", "user-7"));

        assertThat(task.getPriority()).isEqualTo(TaskPriority.CRITICAL);
        assertThat(task.getCategory()).isEqualTo(TaskCategory.COMPLIANCE);
        assertThat(task.getAssigneeId()).isEqualTo("user-3");
        assertThat(task.getUpdatedAt()).isEqualTo(ActivityFixtures.AT);
        verify(auditEntryRepository, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void treatsSameStatusAsNoTransition() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        when(taskRepository.findById("a-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.update("a-1", new UpdateActivityCommand(TaskStatus.TODO, null, null, null, "user-7"));

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void deletesExistingActivity() {
        when(taskRepository.deleteById("a-1")).thenReturn(true);

        service.delete("a-1");

        verify(taskRepository, times(1)).deleteById("a-1");
    }

    @Test
    void throwsTypedNotFoundWhenDeletingUnknownId() {
        when(taskRepository.deleteById("a-9999")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("a-9999")).isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "TODO,DONE,true",
        "TODO,BLOCKED,true",
        "IN_PROGRESS,DONE,true",
        "IN_PROGRESS,BLOCKED,true",
        "DONE,BLOCKED,false",
        "DONE,DONE,false",
        "BLOCKED,DONE,false",
        "TODO,IN_PROGRESS,false",
        "DONE,TODO,false",
        "BLOCKED,IN_PROGRESS,false",
    })
    void enforcesTheTransitionMatrix(TaskStatus from, TaskStatus to, boolean allowed) {
        assertThat(ActivityServiceImpl.canTransition(from, to)).isEqualTo(allowed);
    }
}
