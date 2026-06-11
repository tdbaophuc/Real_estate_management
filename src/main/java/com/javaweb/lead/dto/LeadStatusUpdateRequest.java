package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadPipelineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadStatusUpdateRequest(
        @NotNull(message = "status is required")
        LeadPipelineStatus status,
        @Size(max = 1000, message = "reason must not exceed 1000 characters")
        String reason
) {
    public LeadStatusUpdateRequest {
        reason = reason == null || reason.isBlank() ? null : reason.trim();
    }
}
