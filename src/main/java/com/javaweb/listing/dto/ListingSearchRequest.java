package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

public record ListingSearchRequest(
        @Size(max = 200, message = "keyword must not exceed 200 characters")
        String keyword,
        @Positive(message = "propertyTypeId must be positive")
        Long propertyTypeId,
        ListingPurpose purpose,
        @Positive(message = "provinceId must be positive")
        Long provinceId,
        @Positive(message = "districtId must be positive")
        Long districtId,
        @Positive(message = "wardId must be positive")
        Long wardId,
        @DecimalMin(value = "0", message = "minPrice must be at least 0")
        BigDecimal minPrice,
        @DecimalMin(value = "0", message = "maxPrice must be at least 0")
        BigDecimal maxPrice,
        @DecimalMin(value = "0", message = "minArea must be at least 0")
        BigDecimal minArea,
        @DecimalMin(value = "0", message = "maxArea must be at least 0")
        BigDecimal maxArea,
        @Min(value = 0, message = "bedrooms must be at least 0")
        Integer bedrooms,
        @Min(value = 0, message = "bathrooms must be at least 0")
        Integer bathrooms,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public ListingSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "publishedAt" : sortBy;
        direction = direction == null ? Sort.Direction.DESC : direction;
    }

    @AssertTrue(message = "minPrice must not exceed maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    @AssertTrue(message = "minArea must not exceed maxArea")
    public boolean isAreaRangeValid() {
        return minArea == null || maxArea == null || minArea.compareTo(maxArea) <= 0;
    }
}
