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
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;

/** All programme business logic. */
@Service
public class ProgrammeServiceImpl implements ProgrammeService {

    private final ProjectRepository projectRepository;
    private final StoreOpsEventBus eventBus;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param projectRepository programme store
     * @param eventBus the only legal route for a cross-module side effect
     * @param clock injected clock
     */
    public ProgrammeServiceImpl(ProjectRepository projectRepository, StoreOpsEventBus eventBus, Clock clock) {
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
        this.clock = clock;
    }

    @Override
    public List<Project> listForStore(String storeId) {
        return projectRepository.findByStoreId(storeId);
    }

    @Override
    public Project findById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "Programme " + id + " does not exist"));
    }

    @Override
    public Project create(String name, String storeId, String ownerId) {
        Project project = new Project(UUID.randomUUID().toString(), name, storeId, ownerId, clock.instant());
        return projectRepository.save(project);
    }

    @Override
    public ProjectMember addMember(String projectId, String userId, ProjectRole role) {
        Project project = findById(projectId);
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ConflictException("CONFLICT", "Programme " + projectId + " is closed");
        }
        for (ProjectMember existing : projectRepository.findMembers(projectId)) {
            if (existing.userId().equals(userId)) {
                throw new ConflictException("CONFLICT", "User " + userId + " is already a member");
            }
        }
        return projectRepository.addMember(new ProjectMember(projectId, userId, role));
    }

    @Override
    public Project close(String projectId, String closedByUserId) {
        Project project = findById(projectId);
        project.setStatus(ProjectStatus.CLOSED);
        projectRepository.save(project);
        eventBus.publish(new ProgrammeClosedEvent(projectId, project.getStoreId(),
                closedByUserId, clock.instant()));
        return project;
    }
}
