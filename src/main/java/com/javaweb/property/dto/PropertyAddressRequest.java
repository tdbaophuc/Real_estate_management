package com.javaweb.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyAddressRequest(
        @NotNull(message = "provinceId is required")
        @Positive(message = "provinceId must be positive")
        Long provinceId,

        @Positive(message = "districtId must be positive")
        Long districtId,

        @Positive(message = "wardId must be positive")
        Long wardId,

        @NotBlank(message = "streetAddress is required")
        @Size(max = 255, message = "streetAddress must not exceed 255 characters")
        String streetAddress,

        @Size(max = 500, message = "fullAddress must not exceed 500 characters")
        String fullAddress,

        @Digits(integer = 3, fraction = 7, message = "latitude must contain at most 7 decimal places")
        @DecimalMin(value = "-90.0", message = "latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "latitude must not exceed 90")
        BigDecimal latitude,

        @Digits(integer = 3, fraction = 7, message = "longitude must contain at most 7 decimal places")
        @DecimalMin(value = "-180.0", message = "longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "longitude must not exceed 180")
        BigDecimal longitude
) {
    public PropertyAddressRequest {
        streetAddress = trim(streetAddress);
        fullAddress = trimToNull(fullAddress);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
