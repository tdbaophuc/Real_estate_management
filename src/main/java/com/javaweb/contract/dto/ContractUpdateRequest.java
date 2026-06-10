package com.javaweb.contract.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractUpdateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 250, message = "title must not exceed 250 characters")
        String title,
        @DecimalMin(value = "0.0", message = "totalValue must not be negative")
        BigDecimal totalValue,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        String terms,
        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
    public ContractUpdateRequest {
        title = trim(title);
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        terms = trimToNull(terms);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "expirationDate must be on or after effectiveDate")
    public boolean hasValidDateRange() {
        return effectiveDate == null || expirationDate == null
                || !expirationDate.isBefore(effectiveDate);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
