package com.javaweb.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record AuditLogSearchRequest(
        @Positive Long actorId,
        String action,
        String resourceType,
        @Positive Long resourceId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public AuditLogSearchRequest {
        action = normalize(action);
        resourceType = normalize(resourceType);
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank()
                ? "createdAt"
                : sortBy.trim();
        direction = direction == null ? Sort.Direction.DESC : direction;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase();
    }
}
