package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LeadActivityRequest(
        @NotNull(message = "activityType is required")
        LeadActivityType activityType,
        @Size(max = 250, message = "subject must not exceed 250 characters")
        String subject,
        @Size(max = 4000, message = "details must not exceed 4000 characters")
        String details,
        @PastOrPresent(message = "occurredAt must not be in the future")
        Instant occurredAt
) {
    public LeadActivityRequest {
        subject = subject == null || subject.isBlank() ? null : subject.trim();
        details = details == null || details.isBlank() ? null : details.trim();
    }
}
