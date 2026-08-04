package com.javaweb.property.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyImageUpdateRequest(
        @Size(max = 255, message = "altText must not exceed 255 characters")
        String altText,

        @NotNull(message = "displayOrder is required")
        @Min(value = 0, message = "displayOrder must be at least 0")
        Integer displayOrder
) {
    public PropertyImageUpdateRequest {
        if (altText != null) {
            altText = altText.trim();
            if (altText.isEmpty()) {
                altText = null;
            }
        }
    }
}
