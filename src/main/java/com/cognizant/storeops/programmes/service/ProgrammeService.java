package com.cognizant.storeops.programmes.service;

import java.util.List;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;

/** Business logic for store programmes. */
public interface ProgrammeService {

    /**
     * Lists programmes for a store.
     *
     * @param storeId store id
     * @return its programmes
     */
    List<Project> listForStore(String storeId);

    /**
     * Finds one programme.
     *
     * @param id programme id
     * @return the programme
     */
    Project findById(String id);

    /**
     * Opens a programme.
     *
     * @param name programme name
     * @param storeId store it runs in
     * @param ownerId staff member who owns it
     * @return the created programme
     */
    Project create(String name, String storeId, String ownerId);

    /**
     * Adds a staff member to a programme.
     *
     * @param projectId programme id
     * @param userId staff member id
     * @param role their role in this programme
     * @return the created membership
     */
    ProjectMember addMember(String projectId, String userId, ProjectRole role);

    /**
     * Closes a programme and raises ProgrammeClosedEvent.
     *
     * @param projectId programme id
     * @param closedByUserId who closed it
     * @return the closed programme
     */
    Project close(String projectId, String closedByUserId);
}
