package com.javaweb.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadNoteRequest(
        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must not exceed 4000 characters")
        String content,
        boolean pinned
) {
    public LeadNoteRequest {
        content = content == null ? null : content.trim();
    }
}
