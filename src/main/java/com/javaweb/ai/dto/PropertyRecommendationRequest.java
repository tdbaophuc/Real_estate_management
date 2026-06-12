package com.javaweb.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyRecommendationRequest(
        @Min(value = 1, message = "maxResults must be at least 1")
        @Max(value = 20, message = "maxResults must not exceed 20")
        Integer maxResults,

        @Min(value = 5, message = "candidateLimit must be at least 5")
        @Max(value = 50, message = "candidateLimit must not exceed 50")
        Integer candidateLimit,

        @Size(max = 1000, message = "naturalLanguageNeed must not exceed 1000 characters")
        String naturalLanguageNeed,

        @Size(max = 20, message = "language must not exceed 20 characters")
        String language
) {
    public PropertyRecommendationRequest {
        maxResults = maxResults == null ? 10 : maxResults;
        candidateLimit = candidateLimit == null ? 30 : candidateLimit;
        naturalLanguageNeed = trimToNull(naturalLanguageNeed);
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
