package com.javaweb.customer.dto;

import java.time.Instant;

public record CustomerTagResponse(
        Long id,
        Long customerId,
        String name,
        String color,
        Long createdById,
        String createdByName,
        Instant createdAt
) {
}
