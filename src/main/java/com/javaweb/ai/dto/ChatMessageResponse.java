package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiMessageRole;
import com.javaweb.ai.enums.AiRequestStatus;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        AiMessageRole role,
        String content,
        AiRequestStatus aiStatus,
        String provider,
        String model,
        String errorMessage,
        Instant createdAt
) {
}
