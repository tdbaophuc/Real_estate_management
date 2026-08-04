package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiConversationStatus;
import com.javaweb.listing.dto.PublicListingResponse;

import java.time.Instant;
import java.util.List;

public record ChatSessionResponse(
        Long id,
        String title,
        AiConversationStatus status,
        Long createdById,
        String createdByName,
        String guestSessionId,
        Instant lastMessageAt,
        Instant createdAt,
        List<ChatMessageResponse> messages,
        List<PublicListingResponse> suggestedListings
) {
}
