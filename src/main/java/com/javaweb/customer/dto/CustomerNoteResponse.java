package com.javaweb.customer.dto;

import java.time.Instant;

public record CustomerNoteResponse(
        Long id,
        Long customerId,
        Long authorId,
        String authorName,
        String content,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt
) {
}
