package com.javaweb.lead.dto;

import java.time.Instant;

public record LeadNoteResponse(
        Long id,
        Long leadId,
        Long authorId,
        String authorName,
        String content,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt
) {
}
