package com.javaweb.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must not exceed 2000 characters")
        String content
) {
    public ChatMessageRequest {
        content = content == null ? null : content.trim();
    }
}
