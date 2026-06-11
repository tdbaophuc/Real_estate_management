package com.javaweb.notification.email;

import com.javaweb.auth.entity.User;
import com.javaweb.notification.entity.NotificationTemplate;

import java.time.Instant;
import java.util.Map;

public record EmailDeliveryRequest(
        NotificationTemplate template,
        User recipientUser,
        String recipientEmail,
        String referenceType,
        Long referenceId,
        Instant scheduledAt,
        Map<String, String> variables
) {
}
