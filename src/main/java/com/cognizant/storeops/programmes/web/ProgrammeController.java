package com.cognizant.storeops.programmes.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.service.ProgrammeService;

/** HTTP surface for store programmes. */
@RestController
@RequestMapping("/api/programmes")
public class ProgrammeController {

    private final ProgrammeService programmeService;

    /**
     * Creates the controller.
     *
     * @param programmeService programmes service
     */
    public ProgrammeController(ProgrammeService programmeService) {
        this.programmeService = programmeService;
    }

    /**
     * Lists programmes for a store.
     *
     * @param storeId store to list for
     * @return its programmes
     */
    @GetMapping
    public List<ProgrammeResponse> list(@RequestParam String storeId) {
        return programmeService.listForStore(storeId).stream().map(ProgrammeResponse::from).toList();
    }

    /**
     * Opens a programme.
     *
     * @param request what to create
     * @return 201 with a Location header
     */
    @PostMapping
    public ResponseEntity<ProgrammeResponse> create(@Valid @RequestBody CreateProgrammeRequest request) {
        Project created = programmeService.create(request.name(), request.storeId(), request.ownerId());
        return ResponseEntity.created(URI.create("/api/programmes/" + created.getId()))
                .body(ProgrammeResponse.from(created));
    }

    /**
     * Adds a staff member to a programme.
     *
     * @param id programme id
     * @param request who to add and in what role
     * @return 201 with the membership
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable String id,
            @Valid @RequestBody AddMemberRequest request) {
        ProjectMember member = programmeService.addMember(id, request.userId(), request.role());
        return ResponseEntity.created(URI.create("/api/programmes/" + id + "/members"))
                .body(MemberResponse.from(member));
    }
}
