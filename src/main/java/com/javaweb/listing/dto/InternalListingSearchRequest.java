package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

public record InternalListingSearchRequest(
        ListingStatus status,
        ListingPurpose purpose,
        @Positive(message = "createdBy must be positive")
        Long createdBy,
        @Positive(message = "propertyId must be positive")
        Long propertyId,
        @Size(max = 200, message = "keyword must not exceed 200 characters")
        String keyword,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction sortDirection
) {
    public InternalListingSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        sortDirection = sortDirection == null ? Sort.Direction.DESC : sortDirection;
    }
}
