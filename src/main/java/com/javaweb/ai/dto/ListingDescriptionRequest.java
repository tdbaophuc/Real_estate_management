package com.javaweb.ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ListingDescriptionRequest(
        Long propertyId,
        Long listingId,

        @Size(max = 10, message = "sellingPoints must not contain more than 10 items")
        List<@Size(max = 150, message = "selling point must not exceed 150 characters") String> sellingPoints,

        @Size(max = 50, message = "tone must not exceed 50 characters")
        String tone,

        @Size(max = 20, message = "language must not exceed 20 characters")
        String language
) {
    public ListingDescriptionRequest {
        sellingPoints = sellingPoints == null
                ? List.of()
                : sellingPoints.stream()
                .map(ListingDescriptionRequest::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .toList();
        tone = trimToNull(tone);
        language = trimToNull(language);
    }

    @AssertTrue(message = "propertyId or listingId is required")
    public boolean hasSource() {
        return propertyId != null || listingId != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
