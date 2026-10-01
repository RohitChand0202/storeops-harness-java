package com.cognizant.storeops.activities.repository;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.AuditEntry;
import com.cognizant.storeops.activities.domain.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuditEntryRepositoryTest {

    private final InMemoryAuditEntryRepository repository = new InMemoryAuditEntryRepository();

    @Test
    void appendsAndFindsByTaskId() {
        repository.save(new AuditEntry("e-1", "a-1", TaskStatus.TODO, TaskStatus.DONE,
                "user-7", null, ActivityFixtures.AT));
        repository.save(new AuditEntry("e-2", "a-2", TaskStatus.TODO, TaskStatus.BLOCKED,
                "user-7", "pallet missing", ActivityFixtures.AT));

        assertThat(repository.findByTaskId("a-1")).extracting(AuditEntry::id).containsExactly("e-1");
        assertThat(repository.findByTaskId("a-2")).extracting(AuditEntry::note)
                .containsExactly("pallet missing");
        assertThat(repository.findByTaskId("a-3")).isEmpty();
    }
}
