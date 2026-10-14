package com.cognizant.storeops.alerts.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.web.ApiAuthenticationInterceptor;

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
     * @param caller authenticated recipient
     * @return their alerts
     */
    @GetMapping
    public List<NotificationResponse> list(
            @RequestAttribute(ApiAuthenticationInterceptor.PRINCIPAL_ATTRIBUTE) StoreOpsUser caller) {
        return notificationService.listForUser(caller.id()).stream()
                .map(NotificationResponse::from).toList();
    }
}
