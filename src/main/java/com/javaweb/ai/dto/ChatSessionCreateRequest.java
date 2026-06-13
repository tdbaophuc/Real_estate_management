package com.javaweb.ai.dto;

import jakarta.validation.constraints.Size;

public record ChatSessionCreateRequest(
        @Size(max = 150, message = "title must not exceed 150 characters")
        String title
) {
    public ChatSessionCreateRequest {
        title = trimToNull(title);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
