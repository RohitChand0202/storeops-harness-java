package com.cognizant.storeops.programmes.repository;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.domain.ProjectStatus;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryProjectRepositoryTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private final InMemoryProjectRepository repository = new InMemoryProjectRepository();

    @Test
    void savesAndFindsByStore() {
        Project project = new Project("prog-1", "Autumn reset", "store-12", "user-1", AT);
        project.setDescription("seasonal");
        repository.save(project);

        assertThat(repository.findById("prog-1")).contains(project);
        assertThat(repository.findByStoreId("store-12")).containsExactly(project);
        assertThat(repository.findByStoreId("store-99")).isEmpty();
        assertThat(repository.findById("prog-9")).isEmpty();
        assertThat(project.getDescription()).isEqualTo("seasonal");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(project.getOwnerId()).isEqualTo("user-1");
    }

    @Test
    void addsAndFindsMembersPerProgramme() {
        repository.addMember(new ProjectMember("prog-1", "user-7", ProjectRole.ASSOCIATE));
        repository.addMember(new ProjectMember("prog-2", "user-8", ProjectRole.STORE_MANAGER));

        assertThat(repository.findMembers("prog-1")).extracting(ProjectMember::userId)
                .containsExactly("user-7");
        assertThat(repository.findMembers("prog-3")).isEmpty();
    }

    @Test
    void renamesAProgramme() {
        Project project = new Project("prog-1", "Autumn reset", "store-12", "user-1", AT);
        repository.save(project);
        project.setName("Autumn reset 2026");

        assertThat(repository.findById("prog-1").orElseThrow().getName()).isEqualTo("Autumn reset 2026");
    }
}
