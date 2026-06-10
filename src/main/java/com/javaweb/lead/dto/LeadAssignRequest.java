package com.javaweb.lead.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LeadAssignRequest(
        @NotNull(message = "agentId is required")
        @Positive(message = "agentId must be positive")
        Long agentId,
        @Size(max = 1000, message = "notes must not exceed 1000 characters")
        String notes
) {
    public LeadAssignRequest {
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
