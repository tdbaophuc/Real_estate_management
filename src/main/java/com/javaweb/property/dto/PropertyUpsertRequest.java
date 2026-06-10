package com.javaweb.property.dto;

import com.javaweb.property.enums.FurnitureStatus;
import com.javaweb.property.enums.PropertyDirection;
import com.javaweb.property.enums.PropertyLegalStatus;
import com.javaweb.property.enums.PropertyPurpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyUpsertRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code may only contain letters, digits, underscores and hyphens"
        )
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @Size(max = 10000, message = "description must not exceed 10000 characters")
        String description,

        @NotNull(message = "propertyTypeId is required")
        @Positive(message = "propertyTypeId must be positive")
        Long propertyTypeId,

        @NotNull(message = "purpose is required")
        PropertyPurpose purpose,

        @DecimalMin(value = "0.0", inclusive = true, message = "price must not be negative")
        @Digits(integer = 17, fraction = 2, message = "price must contain at most 17 integer and 2 decimal digits")
        BigDecimal price,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency,

        @DecimalMin(value = "0.0", inclusive = false, message = "landArea must be positive")
        @Digits(integer = 10, fraction = 2, message = "landArea must contain at most 10 integer and 2 decimal digits")
        BigDecimal landArea,

        @DecimalMin(value = "0.0", inclusive = false, message = "floorArea must be positive")
        @Digits(integer = 10, fraction = 2, message = "floorArea must contain at most 10 integer and 2 decimal digits")
        BigDecimal floorArea,

        @PositiveOrZero(message = "bedrooms must not be negative")
        Integer bedrooms,

        @PositiveOrZero(message = "bathrooms must not be negative")
        Integer bathrooms,

        @PositiveOrZero(message = "floors must not be negative")
        Integer floors,

        PropertyDirection direction,

        PropertyLegalStatus legalStatus,

        FurnitureStatus furnitureStatus,

        @Size(max = 500, message = "videoUrl must not exceed 500 characters")
        String videoUrl,

        @Size(max = 500, message = "virtualTourUrl must not exceed 500 characters")
        String virtualTourUrl,

        LocalDate availableFrom,

        @Positive(message = "ownerId must be positive")
        Long ownerId,

        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId,

        @NotNull(message = "address is required")
        @Valid
        PropertyAddressRequest address,

        @Size(max = 100, message = "amenities must not contain more than 100 items")
        List<@Valid PropertyAmenityRequest> amenities
) {
    public PropertyUpsertRequest {
        code = normalizeCode(code);
        name = trim(name);
        description = trimToNull(description);
        currency = currency == null ? null : currency.trim().toUpperCase();
        videoUrl = trimToNull(videoUrl);
        virtualTourUrl = trimToNull(virtualTourUrl);
        amenities = amenities == null ? List.of() : List.copyOf(amenities);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
