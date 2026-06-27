package com.javaweb.listing.dto;

import java.math.BigDecimal;

public record InternalListingPackageSummary(
        Long id,
        String code,
        String name,
        Integer durationDays,
        BigDecimal price,
        boolean active
) {
}
