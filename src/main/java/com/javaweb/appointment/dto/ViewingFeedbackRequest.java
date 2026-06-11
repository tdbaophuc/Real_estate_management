package com.javaweb.appointment.dto;

import com.javaweb.appointment.enums.ViewingInterestLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ViewingFeedbackRequest(
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must not exceed 5")
        Integer rating,
        @NotNull(message = "interestLevel is required")
        ViewingInterestLevel interestLevel,
        @Size(max = 4000, message = "comments must not exceed 4000 characters")
        String comments,
        @Size(max = 2000, message = "positivePoints must not exceed 2000 characters")
        String positivePoints,
        @Size(max = 2000, message = "concerns must not exceed 2000 characters")
        String concerns,
        @Size(max = 1000, message = "nextAction must not exceed 1000 characters")
        String nextAction
) {
    public ViewingFeedbackRequest {
        comments = trimToNull(comments);
        positivePoints = trimToNull(positivePoints);
        concerns = trimToNull(concerns);
        nextAction = trimToNull(nextAction);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
