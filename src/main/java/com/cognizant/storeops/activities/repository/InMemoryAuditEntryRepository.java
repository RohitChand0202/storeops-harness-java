package com.cognizant.storeops.activities.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.activities.domain.AuditEntry;

/** In-memory audit trail. Append-only: there is deliberately no update or delete. */
@Repository
public class InMemoryAuditEntryRepository implements AuditEntryRepository {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public AuditEntry save(AuditEntry entry) {
        entries.add(entry);
        return entry;
    }

    @Override
    public List<AuditEntry> findByTaskId(String taskId) {
        List<AuditEntry> found = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.taskId().equals(taskId)) {
                found.add(entry);
            }
        }
        return List.copyOf(found);
    }
}
