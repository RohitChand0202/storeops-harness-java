package com.cognizant.storeops.activities.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;

/** In-memory activities store. No database by design; this is a reference codebase. */
@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAllById(List<String> ids) {
        List<Task> found = new ArrayList<>();
        for (String id : ids) {
            Task task = tasks.get(id);
            if (task != null) {
                found.add(task);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public List<Task> findAll(String programmeId, TaskStatus status) {
        List<Task> found = new ArrayList<>();
        for (Task task : tasks.values()) {
            boolean programmeMatches = programmeId == null || programmeId.equals(task.getProgrammeId());
            boolean statusMatches = status == null || status == task.getStatus();
            if (programmeMatches && statusMatches) {
                found.add(task);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public Task save(Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public boolean deleteById(String id) {
        return tasks.remove(id) != null;
    }
}
