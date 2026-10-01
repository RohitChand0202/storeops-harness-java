package com.cognizant.storeops.staff.domain;

/** A staff member's role. Drives the store and region scoping every endpoint applies. */
public enum StaffRole {
    REGIONAL_MANAGER,
    STORE_MANAGER,
    DEPARTMENT_LEAD,
    ASSOCIATE
}
