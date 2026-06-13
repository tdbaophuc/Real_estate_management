package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiRequestStatus;

public record AiCompletionResponse(
        AiRequestStatus status,
        String provider,
        String model,
        String content,
        String finishReason,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String errorMessage
) {
    public static AiCompletionResponse skipped(
            String provider,
            String model,
            String message
    ) {
        return new AiCompletionResponse(
                AiRequestStatus.SKIPPED,
                provider,
                model,
                "",
                "SKIPPED",
                null,
                null,
                null,
                message
        );
    }

    public static AiCompletionResponse failed(
            AiRequestStatus status,
            String provider,
            String model,
            String message
    ) {
        return new AiCompletionResponse(
                status,
                provider,
                model,
                "",
                status.name(),
                null,
                null,
                null,
                message
        );
    }
}
