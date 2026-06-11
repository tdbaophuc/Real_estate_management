package com.javaweb.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerNoteRequest(
        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must not exceed 4000 characters")
        String content,
        boolean pinned
) {
    public CustomerNoteRequest {
        content = content == null ? null : content.trim();
    }
}
