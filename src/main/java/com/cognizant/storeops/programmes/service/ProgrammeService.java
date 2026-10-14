package com.cognizant.storeops.programmes.service;

import java.util.List;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.domain.ProjectStatus;
import com.cognizant.storeops.shared.domain.StoreOpsUser;

/** Business logic for store programmes. */
public interface ProgrammeService {

    /**
     * Lists programmes for a store.
     *
     * @param storeId store id
     * @return its programmes
     */
    List<Project> listForStore(StoreOpsUser caller, String storeId);

    /**
     * Finds one programme.
     *
     * @param id programme id
     * @return the programme
     */
    Project findById(String id);

    /** Finds one programme only when it belongs to the caller's store. */
    Project findForCaller(StoreOpsUser caller, String id);

    /**
     * Opens a programme.
     *
     * @param name programme name
     * @param storeId store it runs in
     * @param ownerId staff member who owns it
     * @return the created programme
     */
    Project create(StoreOpsUser caller, String name, String storeId, String ownerId);

    /**
     * Adds a staff member to a programme.
     *
     * @param projectId programme id
     * @param userId staff member id
     * @param role their role in this programme
     * @return the created membership
     */
    ProjectMember addMember(StoreOpsUser caller, String projectId, String userId, ProjectRole role);

    /** Finds a membership only when the caller may see its programme. */
    ProjectMember findMember(StoreOpsUser caller, String projectId, String userId);

    /**
     * Closes a programme and raises ProgrammeClosedEvent.
     *
     * @param projectId programme id
     * @param caller authenticated staff member requesting closure
     * @param targetStatus requested transition target
     * @return the closed programme
     */
    Project close(StoreOpsUser caller, String projectId, ProjectStatus targetStatus);
}
