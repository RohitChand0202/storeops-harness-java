package com.cognizant.storeops.activities;

import java.time.Instant;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.domain.TaskStatus;

/** Per-module fixtures. Fixed instants only: a test that passes in one timezone is a flake. */
public final class ActivityFixtures {

    public static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private ActivityFixtures() {
    }

    public static Task task(String id, TaskStatus status) {
        return new Task(id, "Restock aisle 4", "store-12", status, TaskPriority.HIGH,
                TaskCategory.RESTOCKING, AT);
    }
}
