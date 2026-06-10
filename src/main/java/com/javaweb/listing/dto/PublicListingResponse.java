package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PublicListingResponse(
        Long id,
        String code,
        Long propertyId,
        String propertyCode,
        String propertyName,
        Long propertyTypeId,
        String propertyTypeName,
        String title,
        String slug,
        String description,
        ListingPurpose purpose,
        ListingStatus status,
        BigDecimal askingPrice,
        String currency,
        BigDecimal landArea,
        BigDecimal floorArea,
        Integer bedrooms,
        Integer bathrooms,
        Long provinceId,
        String provinceName,
        Long districtId,
        String districtName,
        Long wardId,
        String wardName,
        String streetAddress,
        String fullAddress,
        long viewCount,
        Instant publishedAt,
        Instant createdAt
) {
}
