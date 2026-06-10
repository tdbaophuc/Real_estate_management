package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadActivityType;

import java.time.Instant;

public record LeadActivityResponse(
        Long id,
        Long leadId,
        LeadActivityType activityType,
        String subject,
        String details,
        Long actorId,
        String actorName,
        Instant occurredAt,
        Instant createdAt
) {
}
