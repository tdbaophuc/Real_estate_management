package com.javaweb.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiCompletionRequest(
        @NotBlank(message = "operation is required")
        @Size(max = 100, message = "operation must not exceed 100 characters")
        String operation,

        @Size(max = 4000, message = "systemPrompt must not exceed 4000 characters")
        String systemPrompt,

        @NotBlank(message = "userPrompt is required")
        @Size(max = 8000, message = "userPrompt must not exceed 8000 characters")
        String userPrompt,

        @Size(max = 100, message = "referenceType must not exceed 100 characters")
        String referenceType,

        Long referenceId,

        @Size(max = 8000, message = "metadataJson must not exceed 8000 characters")
        String metadataJson
) {
}
