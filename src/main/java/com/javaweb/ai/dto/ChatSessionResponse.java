package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiConversationStatus;

import java.time.Instant;
import java.util.List;

public record ChatSessionResponse(
        Long id,
        String title,
        AiConversationStatus status,
        Long createdById,
        String createdByName,
        Instant lastMessageAt,
        Instant createdAt,
        List<ChatMessageResponse> messages
) {
}
