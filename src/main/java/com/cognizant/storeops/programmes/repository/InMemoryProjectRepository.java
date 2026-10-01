package com.cognizant.storeops.programmes.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;

/** In-memory programme store. */
@Repository
public class InMemoryProjectRepository implements ProjectRepository {

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final List<ProjectMember> members = new CopyOnWriteArrayList<>();

    @Override
    public Optional<Project> findById(String id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<Project> findByStoreId(String storeId) {
        List<Project> found = new ArrayList<>();
        for (Project project : projects.values()) {
            if (project.getStoreId().equals(storeId)) {
                found.add(project);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public Project save(Project project) {
        projects.put(project.getId(), project);
        return project;
    }

    @Override
    public ProjectMember addMember(ProjectMember member) {
        members.add(member);
        return member;
    }

    @Override
    public List<ProjectMember> findMembers(String projectId) {
        List<ProjectMember> found = new ArrayList<>();
        for (ProjectMember member : members) {
            if (member.projectId().equals(projectId)) {
                found.add(member);
            }
        }
        return List.copyOf(found);
    }
}
