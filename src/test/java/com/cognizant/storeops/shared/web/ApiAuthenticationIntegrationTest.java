package com.cognizant.storeops.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "storeops.bootstrap.token=integration-test-token",
        "storeops.bootstrap.user-id=integration-user",
        "storeops.bootstrap.email=integration@example.com",
        "storeops.bootstrap.name=Integration User",
        "storeops.bootstrap.role=STORE_MANAGER",
        "storeops.bootstrap.store-id=store-12",
        "storeops.bootstrap.region-id=region-1"})
@AutoConfigureMockMvc
class ApiAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatesTheBootstrappedAccountAndScopesRequestsToItsStore() throws Exception {
        mockMvc.perform(get("/api/programmes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/programmes")
                        .header("Authorization", "Bearer integration-test-token")
                        .param("storeId", "store-99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer integration-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/activities")
                        .header("Authorization", "Bearer integration-test-token")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createdProgrammeLocationCanBeReadWithTheSameToken() throws Exception {
        String location = mockMvc.perform(post("/api/programmes")
                        .header("Authorization", "Bearer integration-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reset\",\"storeId\":\"store-12\","
                                + "\"ownerId\":\"integration-user\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location)
                        .header("Authorization", "Bearer integration-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("integration-user"))
                .andExpect(jsonPath("$.storeId").value("store-12"));

        String memberLocation = mockMvc.perform(post(location + "/members")
                        .header("Authorization", "Bearer integration-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"integration-user\",\"role\":\"STORE_MANAGER\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(memberLocation)
                        .header("Authorization", "Bearer integration-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("integration-user"));

        mockMvc.perform(post(location + "/members")
                        .header("Authorization", "Bearer integration-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"integration-user\",\"role\":\"STORE_MANAGER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
