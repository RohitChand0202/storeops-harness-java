package com.cognizant.storeops.activities.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.activities.domain.AuditEntry;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.repository.AuditEntryRepository;
import com.cognizant.storeops.activities.repository.TaskRepository;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.ForbiddenException;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.shared.event.ShiftHandoverCompletedEvent;
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
    public List<Task> list(StoreOpsUser actor, String programmeId, TaskStatus status) {
        requireActor(actor);
        return taskRepository.findAll(programmeId, status).stream()
                .filter(task -> actor.storeId().equals(task.getStoreId()))
                .toList();
    }

    @Override
    public Task findById(StoreOpsUser actor, String id) {
        requireActor(actor);
        Task task = loadTask(id);
        requireStore(actor, task.getStoreId());
        return task;
    }

    @Override
    public Task create(StoreOpsUser actor, CreateActivityCommand command) {
        requireActor(actor);
        requireStore(actor, command.storeId());
        Task task = new Task(UUID.randomUUID().toString(), command.title(), command.storeId(),
                TaskStatus.TODO, command.priority(), command.category(), clock.instant());
        task.setDescription(command.description());
        task.setProgrammeId(command.programmeId());
        task.setAssigneeId(command.assigneeId());
        return taskRepository.save(task);
    }

    @Override
    public Task update(StoreOpsUser actor, String id, UpdateActivityCommand command) {
        Task task = findById(actor, id);
        synchronized (task) {
            if (!isCurrent(task)) {
                throw new NotFoundException("NOT_FOUND", "Activity " + id + " does not exist");
            }
            TaskStatus previous = task.getStatus();

            if (command.status() != null && command.status() != previous) {
                if (!canTransition(previous, command.status())) {
                    throw new RuleViolationException("INVALID_TRANSITION",
                            "Activity " + id + " cannot move from " + previous + " to " + command.status());
                }
                task.setStatus(command.status());
                auditEntryRepository.save(new AuditEntry(UUID.randomUUID().toString(), id, previous,
                        command.status(), actor.id(), null, clock.instant()));
                if (command.status() == TaskStatus.BLOCKED) {
                    eventBus.publish(new TaskBlockedEvent(id, actor.id(),
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
    }

    @Override
    public BulkStatusResult bulkUpdateStatus(BulkStatusCommand command) {
        StoreOpsUser actor = command.actor();
        if (actor == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication is required");
        }
        if (!isSupportedRole(actor.role())) {
            throw new ForbiddenException("FORBIDDEN", "Role cannot complete a shift handover");
        }

        List<String> ids = command.updates().stream().map(BulkStatusCommand.ItemUpdate::taskId).toList();
        Map<String, Task> tasksById = new HashMap<>();
        for (Task task : taskRepository.findAllById(ids)) {
            tasksById.put(task.getId(), task);
        }

        boolean anyFound = false;
        boolean anyAccessible = false;
        for (BulkStatusCommand.ItemUpdate item : command.updates()) {
            Task task = tasksById.get(item.taskId());
            if (task != null) {
                anyFound = true;
                if (actor.storeId().equals(task.getStoreId())) {
                    anyAccessible = true;
                }
            }
        }
        if (anyFound && !anyAccessible) {
            throw new ForbiddenException("FORBIDDEN", "No requested activity is in the caller's store");
        }

        List<BulkStatusResult.UpdatedItem> succeeded = new ArrayList<>();
        List<BulkStatusResult.FailedItem> failed = new ArrayList<>();
        for (BulkStatusCommand.ItemUpdate item : command.updates()) {
            Task task = tasksById.get(item.taskId());
            if (task == null) {
                failed.add(new BulkStatusResult.FailedItem(item.taskId(), "NOT_FOUND"));
                continue;
            }
            synchronized (task) {
                String reason = failureReason(actor, task, item.targetStatus());
                if (reason != null) {
                    failed.add(new BulkStatusResult.FailedItem(item.taskId(), reason));
                    continue;
                }
                if (!isCurrent(task)) {
                    failed.add(new BulkStatusResult.FailedItem(item.taskId(), "NOT_FOUND"));
                    continue;
                }

                TaskStatus previous = task.getStatus();
                Instant occurredAt = clock.instant();
                task.setStatus(item.targetStatus());
                task.setUpdatedAt(occurredAt);
                taskRepository.save(task);
                auditEntryRepository.save(new AuditEntry(UUID.randomUUID().toString(), item.taskId(),
                        previous, item.targetStatus(), actor.id(), item.note(), occurredAt));
                succeeded.add(new BulkStatusResult.UpdatedItem(item.taskId(), previous, item.targetStatus()));
            }
        }

        BulkStatusResult result = new BulkStatusResult(List.copyOf(succeeded), List.copyOf(failed));
        if (!result.succeeded().isEmpty()) {
            eventBus.publish(new ShiftHandoverCompletedEvent(actor.storeId(), actor.id(),
                    result.succeeded().stream().map(BulkStatusResult.UpdatedItem::taskId).toList(),
                    clock.instant()));
        }
        return result;
    }

    private String failureReason(StoreOpsUser actor, Task task, TaskStatus target) {
        if (!actor.storeId().equals(task.getStoreId())) {
            return "FORBIDDEN";
        }
        if (target != TaskStatus.DONE && target != TaskStatus.BLOCKED) {
            return "UNSUPPORTED_TARGET_STATUS";
        }
        if (task.getStatus() == target) {
            return "ALREADY_IN_STATUS";
        }
        if (!canTransition(task.getStatus(), target)) {
            return "INVALID_TRANSITION";
        }
        return null;
    }

    private boolean isSupportedRole(String role) {
        return "ASSOCIATE".equals(role) || "DEPARTMENT_LEAD".equals(role)
                || "STORE_MANAGER".equals(role) || "REGIONAL_MANAGER".equals(role);
    }

    @Override
    public void delete(StoreOpsUser actor, String id) {
        Task task = findById(actor, id);
        synchronized (task) {
            if (!isCurrent(task) || !taskRepository.deleteById(id)) {
                throw new NotFoundException("NOT_FOUND", "Activity " + id + " does not exist");
            }
        }
    }

    private boolean isCurrent(Task task) {
        return taskRepository.findById(task.getId()).orElse(null) == task;
    }

    private Task loadTask(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "Activity " + id + " does not exist"));
    }

    private void requireActor(StoreOpsUser actor) {
        if (actor == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication is required");
        }
        if (!isSupportedRole(actor.role())) {
            throw new ForbiddenException("FORBIDDEN", "Role cannot act on activities");
        }
    }

    private void requireStore(StoreOpsUser actor, String storeId) {
        if (!actor.storeId().equals(storeId)) {
            throw new ForbiddenException("FORBIDDEN", "Activity is outside the caller's store");
        }
    }

    /**
     * Whether a status transition is legal. A TODO activity may start as IN_PROGRESS; TODO and
     * IN_PROGRESS may reach DONE or BLOCKED. The bulk handover separately restricts its targets.
     *
     * @param from current status
     * @param to target status
     * @return true if the transition is allowed
     */
    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == TaskStatus.TODO && to == TaskStatus.IN_PROGRESS) {
            return true;
        }
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
