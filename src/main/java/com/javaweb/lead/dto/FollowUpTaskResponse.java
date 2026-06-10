package com.javaweb.lead.dto;

import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPriority;

import java.time.Instant;

public record FollowUpTaskResponse(
        Long id,
        Long leadId,
        String title,
        String description,
        FollowUpTaskStatus status,
        LeadPriority priority,
        Long assignedToId,
        String assignedToName,
        Long createdById,
        String createdByName,
        Instant dueAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
