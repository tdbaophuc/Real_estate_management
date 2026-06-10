package com.javaweb.customer.dto;

import java.time.Instant;

public record CustomerTimelineItemResponse(
        String type,
        Long referenceId,
        String title,
        String description,
        Long actorId,
        String actorName,
        Instant occurredAt
) {
}
