package com.javaweb.appointment.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentRescheduleRequest(
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
    public AppointmentRescheduleRequest {
        timezone = trimToNull(timezone);
        meetingLocation = trimToNull(meetingLocation);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "endAt must be after startAt")
    public boolean hasValidTimeRange() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
