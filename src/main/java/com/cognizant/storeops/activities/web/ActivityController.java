package com.cognizant.storeops.activities.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.ActivityService;

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
     * @return matching activities
     */
    @GetMapping
    public List<ActivityResponse> list(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) TaskStatus status) {
        return activityService.list(programmeId, status).stream().map(ActivityResponse::from).toList();
    }

    /**
     * Fetches one activity.
     *
     * @param id activity id
     * @return the activity
     */
    @GetMapping("/{id}")
    public ActivityResponse get(@PathVariable String id) {
        return ActivityResponse.from(activityService.findById(id));
    }

    /**
     * Raises an activity.
     *
     * @param request what to create
     * @return 201 with a Location header
     */
    @PostMapping
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody CreateActivityRequest request) {
        Task created = activityService.create(request.toCommand());
        return ResponseEntity.created(URI.create("/api/activities/" + created.getId()))
                .body(ActivityResponse.from(created));
    }

    /**
     * Applies a partial update.
     *
     * @param id activity id
     * @param request the fields to change
     * @return the updated activity
     */
    @PatchMapping("/{id}")
    public ActivityResponse update(@PathVariable String id,
            @Valid @RequestBody UpdateActivityRequest request) {
        return ActivityResponse.from(activityService.update(id, request.toCommand()));
    }

    /**
     * Removes an activity.
     *
     * @param id activity id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
