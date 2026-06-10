package com.javaweb.lead.dto;

import java.time.Instant;

public record LeadAssignmentResponse(
        Long id,
        Long leadId,
        Long assignedToId,
        String assignedToName,
        Long assignedById,
        String assignedByName,
        Instant assignedAt,
        Instant unassignedAt,
        boolean active,
        String notes
) {
}
