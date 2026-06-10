package com.javaweb.appointment.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code may only contain letters, digits, underscores and hyphens"
        )
        String code,
        @NotNull(message = "customerId is required")
        @Positive(message = "customerId must be positive")
        Long customerId,
        @NotNull(message = "agentId is required")
        @Positive(message = "agentId must be positive")
        Long agentId,
        @NotNull(message = "propertyId is required")
        @Positive(message = "propertyId must be positive")
        Long propertyId,
        @Positive(message = "listingId must be positive")
        Long listingId,
        @Positive(message = "leadId must be positive")
        Long leadId,
        @NotBlank(message = "title is required")
        @Size(max = 250, message = "title must not exceed 250 characters")
        String title,
        @NotNull(message = "startAt is required")
        @Future(message = "startAt must be in the future")
        Instant startAt,
        @NotNull(message = "endAt is required")
        @Future(message = "endAt must be in the future")
        Instant endAt,
        @Size(max = 50, message = "timezone must not exceed 50 characters")
        String timezone,
        @Size(max = 500, message = "meetingLocation must not exceed 500 characters")
        String meetingLocation,
        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
    public AppointmentCreateRequest {
        code = normalizeCode(code);
        title = trim(title);
        timezone = trimToNull(timezone);
        meetingLocation = trimToNull(meetingLocation);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "endAt must be after startAt")
    public boolean hasValidTimeRange() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
