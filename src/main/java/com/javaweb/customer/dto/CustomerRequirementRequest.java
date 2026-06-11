package com.javaweb.customer.dto;

import com.javaweb.listing.enums.ListingPurpose;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerRequirementRequest(
        @NotNull(message = "purpose is required")
        ListingPurpose purpose,
        @Positive(message = "propertyTypeId must be positive")
        Long propertyTypeId,
        @Positive(message = "provinceId must be positive")
        Long provinceId,
        @Positive(message = "districtId must be positive")
        Long districtId,
        @Positive(message = "wardId must be positive")
        Long wardId,
        @DecimalMin(value = "0", message = "minBudget must not be negative")
        @Digits(integer = 17, fraction = 2)
        BigDecimal minBudget,
        @DecimalMin(value = "0", message = "maxBudget must not be negative")
        @Digits(integer = 17, fraction = 2)
        BigDecimal maxBudget,
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency,
        @DecimalMin(value = "0", message = "minArea must not be negative")
        @Digits(integer = 10, fraction = 2)
        BigDecimal minArea,
        @DecimalMin(value = "0", message = "maxArea must not be negative")
        @Digits(integer = 10, fraction = 2)
        BigDecimal maxArea,
        @PositiveOrZero(message = "minBedrooms must not be negative")
        Integer minBedrooms,
        @PositiveOrZero(message = "minBathrooms must not be negative")
        Integer minBathrooms,
        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description
) {
    public CustomerRequirementRequest {
        currency = currency == null ? null : currency.trim().toUpperCase();
        description = description == null || description.isBlank()
                ? null
                : description.trim();
    }

    @AssertTrue(message = "minBudget must not exceed maxBudget")
    public boolean isBudgetRangeValid() {
        return minBudget == null
                || maxBudget == null
                || minBudget.compareTo(maxBudget) <= 0;
    }

    @AssertTrue(message = "minArea must not exceed maxArea")
    public boolean isAreaRangeValid() {
        return minArea == null || maxArea == null || minArea.compareTo(maxArea) <= 0;
    }
}
