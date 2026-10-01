package com.cognizant.storeops.programmes.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.domain.ProjectStatus;
import com.cognizant.storeops.programmes.repository.ProjectRepository;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.StoreOpsEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgrammeServiceImplTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private ProjectRepository projectRepository;
    private StoreOpsEventBus eventBus;
    private ProgrammeServiceImpl service;

    private Project openProgramme() {
        return new Project("prog-1", "Autumn reset", "store-12", "user-1", AT);
    }

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(StoreOpsEventBus.class);
        service = new ProgrammeServiceImpl(projectRepository, eventBus, Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test
    void listsForStore() {
        Project project = openProgramme();
        when(projectRepository.findByStoreId("store-12")).thenReturn(List.of(project));

        assertThat(service.listForStore("store-12")).containsExactly(project);
    }

    @Test
    void throwsTypedNotFoundForUnknownProgramme() {
        when(projectRepository.findById("prog-9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("prog-9"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("prog-9");
    }

    @Test
    void createsProgrammeOpenAndPersistsIt() {
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Project created = service.create("Autumn reset", "store-12", "user-1");

        assertThat(created.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(created.getName()).isEqualTo("Autumn reset");
        assertThat(created.getCreatedAt()).isEqualTo(AT);
        verify(projectRepository, times(1)).save(created);
    }

    @Test
    void addsMember() {
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(openProgramme()));
        when(projectRepository.findMembers("prog-1")).thenReturn(List.of());
        when(projectRepository.addMember(any())).thenAnswer(i -> i.getArgument(0));

        ProjectMember member = service.addMember("prog-1", "user-7", ProjectRole.ASSOCIATE);

        assertThat(member.userId()).isEqualTo("user-7");
        assertThat(member.role()).isEqualTo(ProjectRole.ASSOCIATE);
    }

    @Test
    void refusesToAddAMemberTwiceAndPersistsNothing() {
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(openProgramme()));
        when(projectRepository.findMembers("prog-1"))
                .thenReturn(List.of(new ProjectMember("prog-1", "user-7", ProjectRole.ASSOCIATE)));

        assertThatThrownBy(() -> service.addMember("prog-1", "user-7", ProjectRole.DEPARTMENT_LEAD))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");

        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void refusesToAddAMemberToAClosedProgramme() {
        Project closed = openProgramme();
        closed.setStatus(ProjectStatus.CLOSED);
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.addMember("prog-1", "user-7", ProjectRole.ASSOCIATE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is closed");

        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void closesProgrammeAndPublishesExactlyOneEvent() {
        Project project = openProgramme();
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.close("prog-1", "user-1");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        ArgumentCaptor<StoreOpsEvent> captor = ArgumentCaptor.forClass(StoreOpsEvent.class);
        verify(eventBus, times(1)).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ProgrammeClosedEvent.class);
        ProgrammeClosedEvent event = (ProgrammeClosedEvent) captor.getValue();
        assertThat(event.programmeId()).isEqualTo("prog-1");
        assertThat(event.storeId()).isEqualTo("store-12");
        assertThat(event.closedByUserId()).isEqualTo("user-1");
        assertThat(event.eventType()).isEqualTo("PROGRAMME_CLOSED");
    }
}
