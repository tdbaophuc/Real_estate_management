package com.javaweb.ai.dto;

import jakarta.validation.constraints.Size;

public record LeadScoreRequest(
        @Size(max = 20, message = "language must not exceed 20 characters")
        String language
) {
    public LeadScoreRequest {
        language = trimToNull(language);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
