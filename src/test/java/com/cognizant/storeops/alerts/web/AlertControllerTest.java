package com.cognizant.storeops.alerts.web;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.Notification;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.service.NotificationService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void listsAlertsForARecipient() throws Exception {
        when(notificationService.listForUser("user-7")).thenReturn(List.of(
                new Notification("n-1", "user-7", AlertType.SLA_BREACH,
                        NotificationChannel.IN_APP, "late", AT)));

        mockMvc.perform(get("/api/alerts").param("userId", "user-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("n-1"))
                .andExpect(jsonPath("$[0].alertType").value("SLA_BREACH"))
                .andExpect(jsonPath("$[0].channel").value("IN_APP"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].payload").value("late"));
    }

    @Test
    void returnsAnEmptyListRatherThanNullForARecipientWithNoAlerts() throws Exception {
        when(notificationService.listForUser("user-0")).thenReturn(List.of());

        mockMvc.perform(get("/api/alerts").param("userId", "user-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
