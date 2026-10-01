package com.cognizant.storeops.activities.domain;

/** How urgent an activity is. CRITICAL and HIGH drive SLA breach alerting. */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
