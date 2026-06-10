package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingResponse(
        Long id,
        String code,
        Long propertyId,
        String propertyCode,
        String propertyName,
        Long createdById,
        String createdByName,
        Long listingPackageId,
        String listingPackageCode,
        String listingPackageName,
        String title,
        String slug,
        String description,
        ListingPurpose purpose,
        ListingStatus status,
        ListingVisibility visibility,
        BigDecimal askingPrice,
        String currency,
        String seoTitle,
        String seoDescription,
        String seoKeywords,
        long viewCount,
        Instant createdAt,
        Instant updatedAt
) {
}
