package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingStatus;

import java.time.Instant;

public record InternalListingStatusHistoryResponse(
        Long id,
        ListingStatus fromStatus,
        ListingStatus toStatus,
        InternalListingUserSummary changedBy,
        String reason,
        Instant createdAt
) {
}
