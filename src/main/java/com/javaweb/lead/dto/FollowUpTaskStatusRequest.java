package com.javaweb.lead.dto;

import com.javaweb.lead.enums.FollowUpTaskStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FollowUpTaskStatusRequest(
        @NotNull(message = "status is required")
        FollowUpTaskStatus status,
        Instant completedAt
) {
}
