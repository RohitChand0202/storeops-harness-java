package com.cognizant.storeops.programmes.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.staff.service.StaffService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "storeops.bootstrap.token=manager-test-token",
        "storeops.bootstrap.user-id=manager-1",
        "storeops.bootstrap.email=manager@example.com",
        "storeops.bootstrap.name=Manager",
        "storeops.bootstrap.role=STORE_MANAGER",
        "storeops.bootstrap.store-id=store-12",
        "storeops.bootstrap.region-id=region-1",
        "storeops.bootstrap.incoming-lead.token=lead-test-token",
        "storeops.bootstrap.incoming-lead.user-id=lead-1",
        "storeops.bootstrap.incoming-lead.email=lead@example.com",
        "storeops.bootstrap.incoming-lead.name=Incoming Lead"})
@AutoConfigureMockMvc
class ProgrammeCloseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffService staffService;

    @Test
    void onlyTheOwnerCanCloseAndClosurePublishesOneAlert() throws Exception {
        assertThat(staffService.findDepartmentLeadsForStore("store-12"))
                .extracting(user -> user.id()).containsExactly("lead-1");
        assertThat(staffService.resolveToken("lead-test-token").id()).isEqualTo("lead-1");

        String location = mockMvc.perform(post("/api/programmes")
                        .header("Authorization", "Bearer manager-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reset\",\"storeId\":\"store-12\","
                                + "\"ownerId\":\"manager-1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(location)
                        .header("Authorization", "Bearer lead-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(location)
                        .header("Authorization", "Bearer manager-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(patch(location)
                        .header("Authorization", "Bearer manager-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer manager-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alertType").value("INVENTORY"));
    }
}
