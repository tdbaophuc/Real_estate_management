package com.javaweb.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String actionUrl,
        String referenceType,
        Long referenceId,
        String metadataJson,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
