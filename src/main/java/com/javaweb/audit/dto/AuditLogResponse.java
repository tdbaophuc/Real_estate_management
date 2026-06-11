package com.javaweb.audit.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        String actorName,
        String action,
        String resourceType,
        Long resourceId,
        JsonNode oldValue,
        JsonNode newValue,
        Instant createdAt
) {
}
