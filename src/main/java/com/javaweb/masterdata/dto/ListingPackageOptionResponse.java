package com.javaweb.masterdata.dto;

import java.math.BigDecimal;

public record ListingPackageOptionResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int durationDays,
        boolean featured,
        int priorityLevel,
        boolean active
) {
}
