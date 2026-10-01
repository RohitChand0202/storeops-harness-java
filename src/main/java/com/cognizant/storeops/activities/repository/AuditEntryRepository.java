package com.cognizant.storeops.activities.repository;

import java.util.List;

import com.cognizant.storeops.activities.domain.AuditEntry;

/** Append-only store of applied status transitions. Module-private under ARCH-1. */
public interface AuditEntryRepository {

    /**
     * Appends an audit entry.
     *
     * @param entry the entry
     * @return the stored entry
     */
    AuditEntry save(AuditEntry entry);

    /**
     * Lists audit entries for one activity.
     *
     * @param taskId activity id
     * @return its entries, oldest first
     */
    List<AuditEntry> findByTaskId(String taskId);
}
