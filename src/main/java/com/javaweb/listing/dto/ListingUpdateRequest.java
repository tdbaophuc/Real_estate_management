package com.javaweb.listing.dto;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingVisibility;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ListingUpdateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 250, message = "title must not exceed 250 characters")
        String title,

        @NotBlank(message = "slug is required")
        @Size(max = 300, message = "slug must not exceed 300 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "slug must contain lowercase letters, digits and single hyphens"
        )
        String slug,

        @NotBlank(message = "description is required")
        @Size(max = 20000, message = "description must not exceed 20000 characters")
        String description,

        @NotNull(message = "purpose is required")
        ListingPurpose purpose,

        @NotNull(message = "visibility is required")
        ListingVisibility visibility,

        @DecimalMin(value = "0.0", message = "askingPrice must not be negative")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "askingPrice must contain at most 17 integer and 2 decimal digits"
        )
        BigDecimal askingPrice,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency,

        @Positive(message = "listingPackageId must be positive")
        Long listingPackageId,

        @Size(max = 250, message = "seoTitle must not exceed 250 characters")
        String seoTitle,

        @Size(max = 500, message = "seoDescription must not exceed 500 characters")
        String seoDescription,

        @Size(max = 500, message = "seoKeywords must not exceed 500 characters")
        String seoKeywords
) {
    public ListingUpdateRequest {
        title = trim(title);
        slug = normalizeSlug(slug);
        description = trim(description);
        currency = currency == null ? null : currency.trim().toUpperCase();
        seoTitle = trimToNull(seoTitle);
        seoDescription = trimToNull(seoDescription);
        seoKeywords = trimToNull(seoKeywords);
    }

    private static String normalizeSlug(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
