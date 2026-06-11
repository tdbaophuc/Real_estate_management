package com.javaweb.customer.dto;

import com.javaweb.listing.enums.ListingPurpose;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerRequirementResponse(
        Long id,
        Long customerId,
        ListingPurpose purpose,
        Long propertyTypeId,
        String propertyTypeName,
        Long provinceId,
        String provinceName,
        Long districtId,
        String districtName,
        Long wardId,
        String wardName,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        String currency,
        BigDecimal minArea,
        BigDecimal maxArea,
        Integer minBedrooms,
        Integer minBathrooms,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
