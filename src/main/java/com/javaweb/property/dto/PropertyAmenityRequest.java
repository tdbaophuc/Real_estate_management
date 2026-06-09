package com.javaweb.property.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PropertyAmenityRequest(
        @NotNull(message = "amenityId is required")
        @Positive(message = "amenityId must be positive")
        Long amenityId,

        @Size(max = 255, message = "amenity details must not exceed 255 characters")
        String details
) {
    public PropertyAmenityRequest {
        details = trimToNull(details);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
