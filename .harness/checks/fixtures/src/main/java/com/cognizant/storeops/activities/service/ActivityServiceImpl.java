package com.cognizant.storeops.activities.service;

import com.cognizant.storeops.activities.domain.Task;

/** Clean service: no raw throws. Branches left uncovered for the TEST-1 experiment. */
public class ActivityServiceImpl {

    /** Describes a task. */
    public String describe(Task task) {
        return task.id();
    }

    /** Classifies a count. Deliberately four branches, only one covered by the fixture test. */
    public String classify(int count) {
        if (count > 10) {
            return "HIGH";
        }
        if (count > 5) {
            return "MEDIUM";
        }
        if (count > 0) {
            return "LOW";
        }
        return "NONE";
    }
}
