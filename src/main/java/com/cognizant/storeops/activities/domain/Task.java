package com.cognizant.storeops.activities.domain;

import java.time.Instant;

/**
 * An operational activity: a restocking run, a planogram reset, a compliance check, a general store
 * task. The entity is named Task rather than Activity because the module is named for the retail
 * domain and the type keeps the name the codebase already uses.
 *
 * <p>Mutable by design, which is why it is a class rather than a record: a status transition
 * changes an existing activity rather than producing a new one.
 */
public class Task {

    private final String id;
    private final String storeId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskCategory category;
    private String programmeId;
    private String assigneeId;
    private Instant dueDate;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates an activity.
     *
     * @param id stable id
     * @param title short description of the work
     * @param storeId store the work belongs to
     * @param status initial status
     * @param priority how urgent it is
     * @param category what kind of work it is
     * @param createdAt when it was raised
     */
    public Task(String id, String title, String storeId, TaskStatus status, TaskPriority priority,
            TaskCategory category, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.storeId = storeId;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Activity id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Store the activity belongs to.
     *
     * @return the store id
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Short description of the work.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Longer detail, optional.
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
     * Current status.
     *
     * @return the status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Current priority.
     *
     * @return the priority
     */
    public TaskPriority getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     *
     * @param priority the new priority
     */
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    /**
     * What kind of work this is.
     *
     * @return the category
     */
    public TaskCategory getCategory() {
        return category;
    }

    /**
     * Sets the category.
     *
     * @param category the new category
     */
    public void setCategory(TaskCategory category) {
        this.category = category;
    }

    /**
     * Programme this activity belongs to, if any.
     *
     * @return the programme id
     */
    public String getProgrammeId() {
        return programmeId;
    }

    /**
     * Sets the programme.
     *
     * @param programmeId the programme id
     */
    public void setProgrammeId(String programmeId) {
        this.programmeId = programmeId;
    }

    /**
     * Staff member the activity is assigned to.
     *
     * @return the assignee id
     */
    public String getAssigneeId() {
        return assigneeId;
    }

    /**
     * Sets the assignee.
     *
     * @param assigneeId the assignee id
     */
    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    /**
     * When the work is due.
     *
     * @return the due date
     */
    public Instant getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date.
     *
     * @param dueDate the new due date
     */
    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * When the activity was raised.
     *
     * @return the creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * When the activity last changed.
     *
     * @return the update instant
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the update instant.
     *
     * @param updatedAt when the activity changed
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
