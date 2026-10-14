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
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.error.ForbiddenException;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.StoreOpsEvent;
import com.cognizant.storeops.shared.event.StoreOpsEventBus;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.service.StaffService;

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
    private static final StoreOpsUser CALLER = new StoreOpsUser(
            "user-1", "store-12", "region-1", "STORE_MANAGER");

    private ProjectRepository projectRepository;
    private StoreOpsEventBus eventBus;
    private StaffService staffService;
    private ProgrammeServiceImpl service;

    private Project openProgramme() {
        return new Project("prog-1", "Autumn reset", "store-12", "user-1", AT);
    }

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        eventBus = mock(StoreOpsEventBus.class);
        staffService = mock(StaffService.class);
        when(staffService.findById("user-7")).thenReturn(new User("user-7", "lead@example.com",
                "Lead", StaffRole.DEPARTMENT_LEAD, "store-12", "region-1", true));
        service = new ProgrammeServiceImpl(projectRepository, eventBus, staffService,
                Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test
    void listsForStore() {
        Project project = openProgramme();
        when(projectRepository.findByStoreId("store-12")).thenReturn(List.of(project));

        assertThat(service.listForStore(CALLER, "store-12")).containsExactly(project);
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

        Project created = service.create(CALLER, "Autumn reset", "store-12", "user-1");

        assertThat(created.getStatus()).isEqualTo(ProjectStatus.OPEN);
        assertThat(created.getName()).isEqualTo("Autumn reset");
        assertThat(created.getCreatedAt()).isEqualTo(AT);
        verify(projectRepository, times(1)).save(created);
    }

    @Test
    void refusesToListOrCreateForAnotherStoreOrOwner() {
        assertThatThrownBy(() -> service.listForStore(CALLER, "store-99"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.create(CALLER, "Reset", "store-99", "user-1"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.create(CALLER, "Reset", "store-12", "user-7"))
                .isInstanceOf(ForbiddenException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void refusesToReadOrModifyAnotherStoresProgramme() {
        Project foreign = new Project("prog-9", "Foreign", "store-99", "user-9", AT);
        when(projectRepository.findById("prog-9")).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.findForCaller(CALLER, "prog-9"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.addMember(CALLER, "prog-9", "user-7", ProjectRole.ASSOCIATE))
                .isInstanceOf(ForbiddenException.class);
        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void refusesForeignOrInactiveMember() {
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(openProgramme()));
        when(staffService.findById("user-7")).thenReturn(new User("user-7", "lead@example.com",
                "Lead", StaffRole.DEPARTMENT_LEAD, "store-99", "region-2", true));

        assertThatThrownBy(() -> service.addMember(CALLER, "prog-1", "user-7", ProjectRole.ASSOCIATE))
                .isInstanceOf(ForbiddenException.class);
        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void addsMember() {
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(openProgramme()));
        when(projectRepository.findMembers("prog-1")).thenReturn(List.of());
        when(projectRepository.addMember(any())).thenAnswer(i -> i.getArgument(0));

        ProjectMember member = service.addMember(CALLER, "prog-1", "user-7", ProjectRole.ASSOCIATE);

        assertThat(member.userId()).isEqualTo("user-7");
        assertThat(member.role()).isEqualTo(ProjectRole.ASSOCIATE);
    }

    @Test
    void refusesToAddAMemberTwiceAndPersistsNothing() {
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(openProgramme()));
        when(projectRepository.findMembers("prog-1"))
                .thenReturn(List.of(new ProjectMember("prog-1", "user-7", ProjectRole.ASSOCIATE)));

        assertThatThrownBy(() -> service.addMember(CALLER, "prog-1", "user-7", ProjectRole.DEPARTMENT_LEAD))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");

        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void refusesToAddAMemberToAClosedProgramme() {
        Project closed = openProgramme();
        closed.setStatus(ProjectStatus.CLOSED);
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.addMember(CALLER, "prog-1", "user-7", ProjectRole.ASSOCIATE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is closed");

        verify(projectRepository, never()).addMember(any());
    }

    @Test
    void closesProgrammeAndPublishesExactlyOneEvent() {
        Project project = openProgramme();
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.close(CALLER, "prog-1", ProjectStatus.CLOSED);

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

    @Test
    void closingAnAlreadyClosedProgrammeDoesNotRepublishAnEvent() {
        Project closed = openProgramme();
        closed.setStatus(ProjectStatus.CLOSED);
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.close(CALLER, "prog-1", ProjectStatus.CLOSED))
                .isInstanceOf(ConflictException.class);
        verify(eventBus, never()).publish(any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void refusesClosureOutsideStoreOrByAnUnrelatedAssociate() {
        Project project = openProgramme();
        when(projectRepository.findById("prog-1")).thenReturn(Optional.of(project));
        StoreOpsUser foreign = new StoreOpsUser("user-9", "store-99", "region-2", "STORE_MANAGER");
        StoreOpsUser associate = new StoreOpsUser("user-7", "store-12", "region-1", "ASSOCIATE");

        assertThatThrownBy(() -> service.close(foreign, "prog-1", ProjectStatus.CLOSED))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.close(associate, "prog-1", ProjectStatus.CLOSED))
                .isInstanceOf(ForbiddenException.class);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.OPEN);
        verify(projectRepository, never()).save(any());
        verify(eventBus, never()).publish(any());
    }

    @Test
    void refusesAProgrammeUpdateThatIsNotAClosure() {
        assertThatThrownBy(() -> service.close(CALLER, "prog-1", ProjectStatus.OPEN))
                .isInstanceOf(RuleViolationException.class);
        verify(projectRepository, never()).save(any());
        verify(eventBus, never()).publish(any());
    }
}
