package com.cognizant.storeops.activities.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.ActivityService;
import com.cognizant.storeops.activities.service.BulkStatusResult;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.web.ApiAuthenticationInterceptor;

/**
 * HTTP surface for operational activities. Holds routing, binding, validation and status mapping,
 * and no business rules: deciding whether a transition is legal happens in the service, which is
 * the boundary gate ARCH-3 is about.
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    /**
     * Creates the controller.
     *
     * @param activityService activities service
     */
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Lists activities. Filters are optional, never required.
     *
     * @param programmeId programme to filter by, or null
     * @param status status to filter by, or null
     * @param actor authenticated staff member
     * @return matching activities
     */
    @GetMapping
    public List<ActivityResponse> list(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        return activityService.list(actor, programmeId, status).stream()
                .map(ActivityResponse::from).toList();
    }

    /**
     * Fetches one activity.
     *
     * @param id activity id
     * @param actor authenticated staff member
     * @return the activity
     */
    @GetMapping("/{id}")
    public ActivityResponse get(@PathVariable String id,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        return ActivityResponse.from(activityService.findById(actor, id));
    }

    /**
     * Raises an activity.
     *
     * @param request what to create
     * @param actor authenticated staff member
     * @return 201 with a Location header
     */
    @PostMapping
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody CreateActivityRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        Task created = activityService.create(actor, request.toCommand());
        return ResponseEntity.created(URI.create("/api/activities/" + created.getId()))
                .body(ActivityResponse.from(created));
    }

    /**
     * Applies a partial update.
     *
     * @param id activity id
     * @param request the fields to change
     * @param actor authenticated staff member
     * @return the updated activity
     */
    @PatchMapping("/{id}")
    public ActivityResponse update(@PathVariable String id,
            @Valid @RequestBody UpdateActivityRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        return ActivityResponse.from(activityService.update(actor, id, request.toCommand()));
    }

    /**
     * Applies independent status changes for the outgoing shift.
     *
     * @param request bounded list of requested changes
     * @param actor authenticated staff member
     * @return 200 when all changes apply, otherwise 207 with per-item failures
     */
    @PatchMapping("/bulk-status")
    public ResponseEntity<BulkStatusUpdateResponse> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        BulkStatusResult result = activityService.bulkUpdateStatus(request.toCommand(actor));
        HttpStatus status = result.failed().isEmpty() ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(BulkStatusUpdateResponse.from(result));
    }

    /**
     * Removes an activity.
     *
     * @param id activity id
     * @param actor authenticated staff member
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser actor) {
        activityService.delete(actor, id);
        return ResponseEntity.noContent().build();
    }
}
