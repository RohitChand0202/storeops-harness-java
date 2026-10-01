package com.cognizant.storeops.programmes.domain;

import java.time.Instant;

/**
 * A store programme: a seasonal rollout, a compliance drive, a refit. Named Project rather than
 * Programme for the same reason Task is not Activity - the type keeps the name the codebase uses.
 */
public class Project {

    private final String id;
    private final String storeId;
    private final String ownerId;
    private final Instant createdAt;
    private String name;
    private String description;
    private ProjectStatus status;

    /**
     * Creates a programme.
     *
     * @param id stable id
     * @param name programme name
     * @param storeId store it runs in
     * @param ownerId staff member who owns it
     * @param createdAt when it was opened
     */
    public Project(String id, String name, String storeId, String ownerId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.storeId = storeId;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.status = ProjectStatus.OPEN;
    }

    /**
     * Programme id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Store it runs in.
     *
     * @return the store id
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Owner.
     *
     * @return the owner id
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * When it was opened.
     *
     * @return the creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Programme name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Optional detail.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Whether the programme is still open.
     *
     * @return the status
     */
    public ProjectStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
}
