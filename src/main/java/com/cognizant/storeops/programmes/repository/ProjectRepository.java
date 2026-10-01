package com.cognizant.storeops.programmes.repository;

import java.util.List;
import java.util.Optional;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;

/** Data access for programmes. Module-private under gate ARCH-1. */
public interface ProjectRepository {

    /**
     * Finds one programme.
     *
     * @param id programme id
     * @return the programme, or empty
     */
    Optional<Project> findById(String id);

    /**
     * Lists programmes for one store.
     *
     * @param storeId store id
     * @return its programmes
     */
    List<Project> findByStoreId(String storeId);

    /**
     * Stores a programme.
     *
     * @param project the programme
     * @return the stored programme
     */
    Project save(Project project);

    /**
     * Adds a membership.
     *
     * @param member the membership
     * @return the stored membership
     */
    ProjectMember addMember(ProjectMember member);

    /**
     * Lists memberships of one programme.
     *
     * @param projectId programme id
     * @return its members
     */
    List<ProjectMember> findMembers(String projectId);
}
