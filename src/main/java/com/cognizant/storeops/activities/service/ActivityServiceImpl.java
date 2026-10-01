package com.cognizant.storeops.activities.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.activities.domain.AuditEntry;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.repository.AuditEntryRepository;
import com.cognizant.storeops.activities.repository.TaskRepository;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;
import com.cognizant.storeops.shared.event.TaskBlockedEvent;

/**
 * All activity business logic. Knows nothing about HTTP: it returns domain types and throws
 * AppException subclasses, and the controller decides what status those map to.
 */
@Service
public class ActivityServiceImpl implements ActivityService {

    private final TaskRepository taskRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final StoreOpsEventBus eventBus;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param taskRepository activity store
     * @param auditEntryRepository audit trail
     * @param eventBus the only legal route for a cross-module side effect
     * @param clock injected so a due-date test is not a timezone flake
     */
    public ActivityServiceImpl(TaskRepository taskRepository,
            AuditEntryRepository auditEntryRepository,
            StoreOpsEventBus eventBus,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.eventBus = eventBus;
        this.clock = clock;
    }

    @Override
    public List<Task> list(String programmeId, TaskStatus status) {
        return taskRepository.findAll(programmeId, status);
    }

    @Override
    public Task findById(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "Activity " + id + " does not exist"));
    }

    @Override
    public Task create(CreateActivityCommand command) {
        Task task = new Task(UUID.randomUUID().toString(), command.title(), command.storeId(),
                TaskStatus.TODO, command.priority(), command.category(), clock.instant());
        task.setDescription(command.description());
        task.setProgrammeId(command.programmeId());
        task.setAssigneeId(command.assigneeId());
        return taskRepository.save(task);
    }

    @Override
    public Task update(String id, UpdateActivityCommand command) {
        Task task = findById(id);
        TaskStatus previous = task.getStatus();

        if (command.status() != null && command.status() != previous) {
            if (!canTransition(previous, command.status())) {
                throw new RuleViolationException("INVALID_TRANSITION",
                        "Activity " + id + " cannot move from " + previous + " to " + command.status());
            }
            task.setStatus(command.status());
            auditEntryRepository.save(new AuditEntry(UUID.randomUUID().toString(), id, previous,
                    command.status(), command.actingUserId(), null, clock.instant()));
            if (command.status() == TaskStatus.BLOCKED) {
                eventBus.publish(new TaskBlockedEvent(id, command.actingUserId(),
                        "blocked via update", clock.instant()));
            }
        }
        if (command.priority() != null) {
            task.setPriority(command.priority());
        }
        if (command.category() != null) {
            task.setCategory(command.category());
        }
        if (command.assigneeId() != null) {
            task.setAssigneeId(command.assigneeId());
        }
        task.setUpdatedAt(clock.instant());
        return taskRepository.save(task);
    }

    @Override
    public void delete(String id) {
        if (!taskRepository.deleteById(id)) {
            throw new NotFoundException("NOT_FOUND", "Activity " + id + " does not exist");
        }
    }

    /**
     * Whether a status transition is legal. Only TODO and IN_PROGRESS may reach DONE or BLOCKED;
     * handover marks work finished or stuck and never reopens it.
     *
     * @param from current status
     * @param to target status
     * @return true if the transition is allowed
     */
    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (to != TaskStatus.DONE && to != TaskStatus.BLOCKED) {
            return false;
        }
        return from == TaskStatus.TODO || from == TaskStatus.IN_PROGRESS;
    }

    /**
     * Instant the service considers "now".
     *
     * @return the current instant from the injected clock
     */
    protected Instant now() {
        return clock.instant();
    }
}
