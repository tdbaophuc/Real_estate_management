package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record FollowUpTaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 250, message = "title must not exceed 250 characters")
        String title,
        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,
        @NotNull(message = "priority is required")
        LeadPriority priority,
        @NotNull(message = "dueAt is required")
        @Future(message = "dueAt must be in the future")
        Instant dueAt,
        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId
) {
    public FollowUpTaskRequest {
        title = title == null ? null : title.trim();
        description = description == null || description.isBlank()
                ? null
                : description.trim();
    }
}
