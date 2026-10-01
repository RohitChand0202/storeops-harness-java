package com.cognizant.storeops.activities.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.activities.ActivityFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    void carriesEveryFieldItWasBuiltWith() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);

        assertThat(task.getId()).isEqualTo("a-1");
        assertThat(task.getStoreId()).isEqualTo("store-12");
        assertThat(task.getTitle()).isEqualTo("Restock aisle 4");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getCategory()).isEqualTo(TaskCategory.RESTOCKING);
        assertThat(task.getCreatedAt()).isEqualTo(ActivityFixtures.AT);
        assertThat(task.getUpdatedAt()).isEqualTo(ActivityFixtures.AT);
    }

    @Test
    void acceptsMutationOfTheFieldsAnUpdateMayChange() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        Instant later = ActivityFixtures.AT.plusSeconds(60);

        task.setTitle("Restock aisle 5");
        task.setDescription("two pallets");
        task.setStatus(TaskStatus.DONE);
        task.setPriority(TaskPriority.LOW);
        task.setCategory(TaskCategory.AUDIT);
        task.setProgrammeId("prog-2");
        task.setAssigneeId("user-9");
        task.setDueDate(later);
        task.setUpdatedAt(later);

        assertThat(task.getTitle()).isEqualTo("Restock aisle 5");
        assertThat(task.getDescription()).isEqualTo("two pallets");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(task.getCategory()).isEqualTo(TaskCategory.AUDIT);
        assertThat(task.getProgrammeId()).isEqualTo("prog-2");
        assertThat(task.getAssigneeId()).isEqualTo("user-9");
        assertThat(task.getDueDate()).isEqualTo(later);
        assertThat(task.getUpdatedAt()).isEqualTo(later);
    }
}
