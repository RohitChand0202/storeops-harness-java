package com.cognizant.storeops.activities.domain;

/** Activities entity. A domain type is outside ERR-1 as the rule is written. */
public class Task {

    private final String id;

    public Task(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id;
    }

    /** Task id. */
    public String id() {
        return id;
    }
}
