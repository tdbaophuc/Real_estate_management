package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;

import java.time.Instant;

public record LeadResponse(
        Long id,
        String code,
        String fullName,
        String email,
        String phone,
        LeadPipelineStatus status,
        LeadPriority priority,
        Integer score,
        String message,
        String lostReason,
        Long sourceId,
        String sourceCode,
        String sourceName,
        Long customerId,
        String customerName,
        Long listingId,
        String listingTitle,
        Long assignedAgentId,
        String assignedAgentName,
        Long createdById,
        String createdByName,
        Instant lastContactedAt,
        Instant convertedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
