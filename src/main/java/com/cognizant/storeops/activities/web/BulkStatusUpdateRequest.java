package com.cognizant.storeops.activities.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.BulkStatusCommand;
import com.cognizant.storeops.shared.domain.StoreOpsUser;

/**
 * Bounded shift handover request. A valid item can fail its business rules independently.
 *
 * @param updates one to 200 activity changes
 */
public record BulkStatusUpdateRequest(
        @NotEmpty @Size(max = 200) List<@NotNull @Valid ItemUpdate> updates) {

    /** Keeps invalid null values intact so Bean Validation can report them as HTTP 400. */
    public BulkStatusUpdateRequest {
        updates = updates == null ? null : Collections.unmodifiableList(new ArrayList<>(updates));
    }

    /**
     * Returns a snapshot of submitted items, including invalid nulls for validation.
     *
     * @return the submitted updates, or null
     */
    public List<ItemUpdate> updates() {
        return updates == null ? null : Collections.unmodifiableList(new ArrayList<>(updates));
    }

    /**
     * Converts the wire shape to a service command using the authenticated caller.
     *
     * @param actor authenticated staff member
     * @return a command for the activities service
     */
    public BulkStatusCommand toCommand(StoreOpsUser actor) {
        return new BulkStatusCommand(actor, updates.stream()
                .map(item -> new BulkStatusCommand.ItemUpdate(
                        item.taskId(), item.targetStatus(), item.note()))
                .toList());
    }

    /**
     * One requested activity change.
     *
     * @param taskId activity id
     * @param targetStatus requested status
     * @param note optional audit note, at most 500 characters
     */
    public record ItemUpdate(
            @NotBlank String taskId,
            @NotNull TaskStatus targetStatus,
            @Size(max = 500) String note) {
    }
}
