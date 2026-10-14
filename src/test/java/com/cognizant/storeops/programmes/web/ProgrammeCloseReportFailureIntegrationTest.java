package com.cognizant.storeops.programmes.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.service.ReportService;
import com.cognizant.storeops.shared.error.RuleViolationException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "storeops.bootstrap.token=manager-report-failure-token",
        "storeops.bootstrap.user-id=manager-report-failure",
        "storeops.bootstrap.email=manager@example.com",
        "storeops.bootstrap.name=Manager",
        "storeops.bootstrap.role=STORE_MANAGER",
        "storeops.bootstrap.store-id=store-12",
        "storeops.bootstrap.region-id=region-1"})
@AutoConfigureMockMvc
class ProgrammeCloseReportFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void failedReportDoesNotTurnACompletedCloseIntoA500() throws Exception {
        when(reportService.request(ReportType.STORE_SUMMARY, "store-12"))
                .thenThrow(new RuleViolationException("REPORT_UNAVAILABLE", "Report could not be built"));

        String location = mockMvc.perform(post("/api/programmes")
                        .header("Authorization", "Bearer manager-report-failure-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reset\",\"storeId\":\"store-12\","
                                + "\"ownerId\":\"manager-report-failure\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(patch(location)
                        .header("Authorization", "Bearer manager-report-failure-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get(location)
                        .header("Authorization", "Bearer manager-report-failure-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(patch(location)
                        .header("Authorization", "Bearer manager-report-failure-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer manager-report-failure-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
