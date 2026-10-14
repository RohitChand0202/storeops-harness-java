package com.cognizant.storeops.programmes.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.service.ProgrammeService;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.web.ApiAuthenticationInterceptor;

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
     * @param storeId store to list for; defaults to the caller's store
     * @param caller authenticated staff member
     * @return its programmes
     */
    @GetMapping
    public List<ProgrammeResponse> list(@RequestParam(required = false) String storeId,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        String requestedStore = storeId == null ? caller.storeId() : storeId;
        return programmeService.listForStore(caller, requestedStore).stream()
                .map(ProgrammeResponse::from).toList();
    }

    /** Returns the resource addressed by a programme creation Location header. */
    @GetMapping("/{id}")
    public ProgrammeResponse get(@PathVariable String id,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        return ProgrammeResponse.from(programmeService.findForCaller(caller, id));
    }

    /**
     * Opens a programme.
     *
     * @param request what to create
     * @param caller authenticated staff member
     * @return 201 with a Location header
     */
    @PostMapping
    public ResponseEntity<ProgrammeResponse> create(@Valid @RequestBody CreateProgrammeRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        Project created = programmeService.create(caller, request.name(), request.storeId(),
                request.ownerId());
        return ResponseEntity.created(URI.create("/api/programmes/" + created.getId()))
                .body(ProgrammeResponse.from(created));
    }

    /**
     * Adds a staff member to a programme.
     *
     * @param id programme id
     * @param request who to add and in what role
     * @param caller authenticated staff member
     * @return 201 with the membership
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable String id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        ProjectMember member = programmeService.addMember(caller, id, request.userId(), request.role());
        URI location = UriComponentsBuilder.fromPath("/api/programmes/{id}/members/{userId}")
                .buildAndExpand(id, member.userId()).encode().toUri();
        return ResponseEntity.created(location)
                .body(MemberResponse.from(member));
    }

    /** Returns the resource addressed by a member creation Location header. */
    @GetMapping("/{id}/members/{userId}")
    public MemberResponse getMember(@PathVariable String id, @PathVariable String userId,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        return MemberResponse.from(programmeService.findMember(caller, id, userId));
    }

    /** Closes a programme through a scoped state transition. */
    @PatchMapping("/{id}")
    public ProgrammeResponse close(@PathVariable String id,
            @Valid @RequestBody UpdateProgrammeRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        return ProgrammeResponse.from(programmeService.close(caller, id, request.status()));
    }
}
