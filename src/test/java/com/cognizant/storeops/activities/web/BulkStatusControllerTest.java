package com.cognizant.storeops.activities.web;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.ActivityService;
import com.cognizant.storeops.activities.service.BulkStatusCommand;
import com.cognizant.storeops.activities.service.BulkStatusResult;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.staff.service.StaffService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class BulkStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private StaffService staffService;

    @BeforeEach
    void setUpCaller() {
        when(staffService.resolveToken("test-token"))
                .thenReturn(new StoreOpsUser("user-7", "store-12", "region-1", "ASSOCIATE"));
    }

    @Test
    void returns200WithCountsAndAuditableTransitionShape() throws Exception {
        when(activityService.bulkUpdateStatus(any())).thenReturn(new BulkStatusResult(
                List.of(new BulkStatusResult.UpdatedItem("a-1", TaskStatus.TODO, TaskStatus.DONE)),
                List.of()));

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorId\":\"user-evil\",\"updates\":[{\"taskId\":\"a-1\","
                                + "\"targetStatus\":\"DONE\",\"note\":\"done at handover\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.succeededCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.succeeded[0].taskId").value("a-1"))
                .andExpect(jsonPath("$.succeeded[0].previousStatus").value("TODO"))
                .andExpect(jsonPath("$.succeeded[0].newStatus").value("DONE"))
                .andExpect(jsonPath("$.failed.length()").value(0));

        ArgumentCaptor<BulkStatusCommand> captor = ArgumentCaptor.forClass(BulkStatusCommand.class);
        verify(activityService).bulkUpdateStatus(captor.capture());
        assertThat(captor.getValue().actor().id()).isEqualTo("user-7");
        assertThat(captor.getValue().updates().get(0).note()).isEqualTo("done at handover");
    }

    @Test
    void returns207WithBothOutcomeLists() throws Exception {
        when(activityService.bulkUpdateStatus(any())).thenReturn(new BulkStatusResult(
                List.of(new BulkStatusResult.UpdatedItem("a-1", TaskStatus.TODO, TaskStatus.DONE)),
                List.of(new BulkStatusResult.FailedItem("missing", "NOT_FOUND"))));

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"updates\":[{\"taskId\":\"a-1\",\"targetStatus\":\"DONE\"},"
                                + "{\"taskId\":\"missing\",\"targetStatus\":\"BLOCKED\"}]}"))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.succeededCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.succeeded[0].taskId").value("a-1"))
                .andExpect(jsonPath("$.failed[0].taskId").value("missing"))
                .andExpect(jsonPath("$.failed[0].reason").value("NOT_FOUND"));
    }

    @Test
    void rejectsAllFiveInvalidBodyShapesBeforeCallingService() throws Exception {
        String item = "{\"taskId\":\"a-1\",\"targetStatus\":\"DONE\"}";
        String excessiveNote = "a".repeat(501);
        for (String body : List.of(
                "{\"updates\":[]}",
                "{\"updates\":[" + (item + ",").repeat(200) + item + "]}",
                "{\"updates\":[{\"taskId\":\"\",\"targetStatus\":\"DONE\"}]}",
                "{\"updates\":[{\"taskId\":\"a-1\",\"targetStatus\":null}]}",
                "{\"updates\":[{\"taskId\":\"a-1\",\"targetStatus\":\"DONE\","
                        + "\"note\":\"" + excessiveNote + "\"}]}")) {
            mockMvc.perform(patch("/api/activities/bulk-status")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors.length()").value(
                            org.hamcrest.Matchers.greaterThan(0)));
        }

        verify(activityService, never()).bulkUpdateStatus(any());
    }

    @Test
    void rejectsNullUpdatesAndNullItemThroughBeanValidation() throws Exception {
        for (String body : List.of("{\"updates\":null}", "{\"updates\":[null]}")) {
            mockMvc.perform(patch("/api/activities/bulk-status")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        verify(activityService, never()).bulkUpdateStatus(any());
    }

    @Test
    void returns207WhenEveryItemHasFailed() throws Exception {
        when(activityService.bulkUpdateStatus(any())).thenReturn(new BulkStatusResult(
                List.of(), List.of(new BulkStatusResult.FailedItem("a-1", "ALREADY_IN_STATUS"),
                        new BulkStatusResult.FailedItem("missing", "NOT_FOUND"))));

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"updates\":[{\"taskId\":\"a-1\",\"targetStatus\":\"DONE\"},"
                                + "{\"taskId\":\"missing\",\"targetStatus\":\"DONE\"}]}"))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.succeededCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.failed[0].taskId").value("a-1"))
                .andExpect(jsonPath("$.failed[0].reason").value("ALREADY_IN_STATUS"))
                .andExpect(jsonPath("$.failed[1].taskId").value("missing"))
                .andExpect(jsonPath("$.failed[1].reason").value("NOT_FOUND"));
    }

    @Test
    void rejectsMissingAndInvalidBearerBeforeCallingService() throws Exception {
        String validBody = "{\"updates\":[{\"taskId\":\"a-1\",\"targetStatus\":\"DONE\"}]}";
        when(staffService.resolveToken("bad-token"))
                .thenThrow(new UnauthorizedException("UNAUTHORIZED", "Token is not recognised"));

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("Authorization", "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(activityService, never()).bulkUpdateStatus(any());
    }

}
