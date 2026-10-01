package com.cognizant.storeops.activities.domain;

import java.time.Instant;

/**
 * One applied status transition. Written by the activities service and never updated or deleted:
 * it is the handover record, so a reported success implies exactly one of these and a reported
 * failure implies none.
 *
 * @param id stable id
 * @param taskId activity that changed
 * @param previousStatus status before the transition
 * @param newStatus status after it
 * @param actingUserId who applied it
 * @param note optional free text
 * @param occurredAt when it was applied
 */
public record AuditEntry(
        String id,
        String taskId,
        TaskStatus previousStatus,
        TaskStatus newStatus,
        String actingUserId,
        String note,
        Instant occurredAt) {
}
