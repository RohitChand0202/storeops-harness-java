package com.cognizant.storeops.activities.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cognizant.storeops.activities.domain.Task;

class ActivityServiceImplTest {

    @Test
    void describesTask() {
        assertEquals("a-1", new ActivityServiceImpl().describe(new Task("a-1")));
    }
}
