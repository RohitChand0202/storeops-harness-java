package com.cognizant.storeops.alerts.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.storeops.alerts.service.NotificationService;

/**
 * HTTP surface for alerts. This controller lives inside the alerts module, so depending on
 * NotificationService here is not a cross-module call and ARCH-2 does not apply to it.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final NotificationService notificationService;

    /**
     * Creates the controller.
     *
     * @param notificationService alerts service
     */
    public AlertController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lists alerts for one recipient.
     *
     * @param userId recipient to list for
     * @return their alerts
     */
    @GetMapping
    public List<NotificationResponse> list(@RequestParam String userId) {
        return notificationService.listForUser(userId).stream().map(NotificationResponse::from).toList();
    }
}
