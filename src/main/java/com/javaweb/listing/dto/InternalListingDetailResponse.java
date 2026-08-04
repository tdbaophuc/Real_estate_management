package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InternalListingDetailResponse(
        Long id,
        String code,
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
        String rejectionReason,
        Instant submittedAt,
        Instant reviewedAt,
        Instant publishedAt,
        Instant unpublishedAt,
        Instant expiresAt,
        Instant featuredUntil,
        long viewCount,
        long favoriteCount,
        InternalListingPropertySummary property,
        InternalListingUserSummary creator,
        InternalListingUserSummary reviewer,
        InternalListingPackageSummary listingPackage,
        List<InternalListingStatusHistoryResponse> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {
}
