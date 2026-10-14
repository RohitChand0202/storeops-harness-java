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

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.ActivityService;
import com.cognizant.storeops.activities.service.UpdateActivityCommand;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.RuleViolationException;
import com.cognizant.storeops.staff.service.StaffService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

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
    void listsActivitiesAndMapsEveryFieldOntoTheWire() throws Exception {
        Task task = ActivityFixtures.task("a-1", TaskStatus.IN_PROGRESS);
        task.setProgrammeId("prog-1");
        when(activityService.list(any(StoreOpsUser.class), eq(null), eq(null))).thenReturn(List.of(task));

        mockMvc.perform(get("/api/activities").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("a-1"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].category").value("RESTOCKING"))
                .andExpect(jsonPath("$[0].storeId").value("store-12"))
                .andExpect(jsonPath("$[0].programmeId").value("prog-1"));
    }

    @Test
    void passesBothFiltersThroughToTheService() throws Exception {
        when(activityService.list(any(StoreOpsUser.class), eq("prog-1"), eq(TaskStatus.DONE)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/activities").header("Authorization", "Bearer test-token")
                        .param("programmeId", "prog-1").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(activityService, times(1)).list(any(StoreOpsUser.class), eq("prog-1"), eq(TaskStatus.DONE));
    }

    @Test
    void getsOneActivity() throws Exception {
        when(activityService.findById(any(StoreOpsUser.class), eq("a-1")))
                .thenReturn(ActivityFixtures.task("a-1", TaskStatus.TODO));

        mockMvc.perform(get("/api/activities/a-1").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void mapsATypedNotFoundOntoA404WithItsCode() throws Exception {
        when(activityService.findById(any(StoreOpsUser.class), eq("a-9999")))
                .thenThrow(new NotFoundException("NOT_FOUND", "Activity a-9999 does not exist"));

        mockMvc.perform(get("/api/activities/a-9999").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/activities/a-9999"));
    }

    @Test
    void createsAnActivityAndSetsLocation() throws Exception {
        when(activityService.create(any(StoreOpsUser.class), any()))
                .thenReturn(ActivityFixtures.task("a-new", TaskStatus.TODO));

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Restock aisle 4\",\"storeId\":\"store-12\","
                                + "\"priority\":\"HIGH\",\"category\":\"RESTOCKING\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/activities/a-new"))
                .andExpect(jsonPath("$.id").value("a-new"));
    }

    @Test
    void rejectsAMalformedBodyWithFieldLevelDetailAndCallsNoService() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"storeId\":\"store-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        verify(activityService, never()).create(any(StoreOpsUser.class), any());
    }

    @Test
    void patchesAnActivity() throws Exception {
        Task updated = ActivityFixtures.task("a-1", TaskStatus.DONE);
        when(activityService.update(any(StoreOpsUser.class), eq("a-1"), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/activities/a-1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"actingUserId\":\"user-evil\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        ArgumentCaptor<StoreOpsUser> caller = ArgumentCaptor.forClass(StoreOpsUser.class);
        ArgumentCaptor<UpdateActivityCommand> command = ArgumentCaptor.forClass(UpdateActivityCommand.class);
        verify(activityService).update(caller.capture(), eq("a-1"), command.capture());
        assertThat(caller.getValue().id()).isEqualTo("user-7");
        assertThat(command.getValue().status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void mapsARuleViolationOntoA422WithItsCode() throws Exception {
        when(activityService.update(any(StoreOpsUser.class), eq("a-1042"), any()))
                .thenThrow(new RuleViolationException(
                "INVALID_TRANSITION", "Activity a-1042 cannot move from DONE to BLOCKED"));

        mockMvc.perform(patch("/api/activities/a-1042")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"actingUserId\":\"user-7\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.message").value(
                        "Activity a-1042 cannot move from DONE to BLOCKED"));
    }

    @Test
    void deletesAnActivity() throws Exception {
        mockMvc.perform(delete("/api/activities/a-1").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());

        verify(activityService, times(1)).delete(any(StoreOpsUser.class), eq("a-1"));
    }

    @Test
    void mapsADeleteOfAnUnknownActivityOntoA404() throws Exception {
        doThrow(new NotFoundException("NOT_FOUND", "Activity a-9999 does not exist"))
                .when(activityService).delete(any(StoreOpsUser.class), eq("a-9999"));

        mockMvc.perform(delete("/api/activities/a-9999").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
