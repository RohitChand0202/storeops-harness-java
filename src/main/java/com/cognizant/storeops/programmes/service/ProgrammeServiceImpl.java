package com.cognizant.storeops.programmes.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.domain.ProjectStatus;
import com.cognizant.storeops.programmes.repository.ProjectRepository;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.error.ForbiddenException;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.service.StaffService;

/** All programme business logic. */
@Service
public class ProgrammeServiceImpl implements ProgrammeService {

    private final ProjectRepository projectRepository;
    private final StoreOpsEventBus eventBus;
    private final StaffService staffService;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param projectRepository programme store
     * @param eventBus the only legal route for a cross-module side effect
     * @param staffService read-only staff lookup for membership validation
     * @param clock injected clock
     */
    public ProgrammeServiceImpl(ProjectRepository projectRepository, StoreOpsEventBus eventBus,
            StaffService staffService, Clock clock) {
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
        this.staffService = staffService;
        this.clock = clock;
    }

    @Override
    public List<Project> listForStore(StoreOpsUser caller, String storeId) {
        requireStore(caller, storeId);
        return projectRepository.findByStoreId(storeId);
    }

    @Override
    public Project findById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "Programme " + id + " does not exist"));
    }

    @Override
    public Project findForCaller(StoreOpsUser caller, String id) {
        Project project = findById(id);
        requireStore(caller, project.getStoreId());
        return project;
    }

    @Override
    public Project create(StoreOpsUser caller, String name, String storeId, String ownerId) {
        requireStore(caller, storeId);
        if (!caller.id().equals(ownerId)) {
            throw new ForbiddenException("FORBIDDEN", "Programme owner must be the caller");
        }
        Project project = new Project(UUID.randomUUID().toString(), name, storeId, ownerId, clock.instant());
        return projectRepository.save(project);
    }

    @Override
    public synchronized ProjectMember addMember(StoreOpsUser caller, String projectId, String userId,
            ProjectRole role) {
        Project project = findForCaller(caller, projectId);
        if (!caller.id().equals(project.getOwnerId()) && !"STORE_MANAGER".equals(caller.role())) {
            throw new ForbiddenException("FORBIDDEN", "Only the owner or store manager may add members");
        }
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ConflictException("CONFLICT", "Programme " + projectId + " is closed");
        }
        User memberUser = staffService.findById(userId);
        if (!memberUser.active() || !project.getStoreId().equals(memberUser.storeId())) {
            throw new ForbiddenException("FORBIDDEN", "Member must be active in the programme store");
        }
        for (ProjectMember existing : projectRepository.findMembers(projectId)) {
            if (existing.userId().equals(userId)) {
                throw new ConflictException("CONFLICT", "User " + userId + " is already a member");
            }
        }
        return projectRepository.addMember(new ProjectMember(projectId, userId, role));
    }

    @Override
    public ProjectMember findMember(StoreOpsUser caller, String projectId, String userId) {
        findForCaller(caller, projectId);
        return projectRepository.findMembers(projectId).stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("NOT_FOUND",
                        "Member " + userId + " does not exist in programme " + projectId));
    }

    @Override
    public synchronized Project close(StoreOpsUser caller, String projectId, ProjectStatus targetStatus) {
        if (targetStatus != ProjectStatus.CLOSED) {
            throw new RuleViolationException("INVALID_TRANSITION", "Only a CLOSED transition is supported");
        }
        Project project = findForCaller(caller, projectId);
        if (!caller.id().equals(project.getOwnerId()) && !"STORE_MANAGER".equals(caller.role())) {
            throw new ForbiddenException("FORBIDDEN", "Only the owner or store manager may close a programme");
        }
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ConflictException("CONFLICT", "Programme " + projectId + " is already closed");
        }
        project.setStatus(ProjectStatus.CLOSED);
        projectRepository.save(project);
        eventBus.publish(new ProgrammeClosedEvent(projectId, project.getStoreId(),
                caller.id(), clock.instant()));
        return project;
    }

    private void requireStore(StoreOpsUser caller, String storeId) {
        if (!caller.storeId().equals(storeId)) {
            throw new ForbiddenException("FORBIDDEN", "Programme is outside the caller's store");
        }
    }
}
