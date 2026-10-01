package com.cognizant.storeops.alerts.domain;

/** What operational event an alert arose from. */
public enum AlertType {
    INVENTORY,
    SLA_BREACH,
    SHIFT_HANDOVER,
    ESCALATION
}
