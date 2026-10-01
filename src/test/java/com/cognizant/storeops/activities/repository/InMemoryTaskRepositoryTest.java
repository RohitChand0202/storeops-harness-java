package com.cognizant.storeops.activities.repository;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskRepositoryTest {

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();

    @Test
    void savesAndFindsById() {
        Task task = ActivityFixtures.task("a-1", TaskStatus.TODO);
        repository.save(task);

        assertThat(repository.findById("a-1")).contains(task);
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void findsAllByIdSkippingUnknownIds() {
        repository.save(ActivityFixtures.task("a-1", TaskStatus.TODO));
        repository.save(ActivityFixtures.task("a-2", TaskStatus.DONE));

        assertThat(repository.findAllById(List.of("a-1", "a-9999", "a-2")))
                .extracting(Task::getId).containsExactlyInAnyOrder("a-1", "a-2");
    }

    @Test
    void filtersByProgrammeAndStatusIndependently() {
        Task todo = ActivityFixtures.task("a-1", TaskStatus.TODO);
        todo.setProgrammeId("prog-1");
        Task done = ActivityFixtures.task("a-2", TaskStatus.DONE);
        done.setProgrammeId("prog-2");
        repository.save(todo);
        repository.save(done);

        assertThat(repository.findAll(null, null)).hasSize(2);
        assertThat(repository.findAll("prog-1", null)).extracting(Task::getId).containsExactly("a-1");
        assertThat(repository.findAll(null, TaskStatus.DONE)).extracting(Task::getId).containsExactly("a-2");
        assertThat(repository.findAll("prog-1", TaskStatus.DONE)).isEmpty();
    }

    @Test
    void deletesOnlyWhatExists() {
        repository.save(ActivityFixtures.task("a-1", TaskStatus.TODO));

        assertThat(repository.deleteById("a-1")).isTrue();
        assertThat(repository.deleteById("a-1")).isFalse();
        assertThat(repository.findById("a-1")).isEmpty();
    }
}
