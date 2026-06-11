package com.javaweb.contract.dto;

import com.javaweb.contract.enums.ContractType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code may only contain letters, digits, underscores and hyphens"
        )
        String code,
        @NotNull(message = "contractType is required")
        ContractType contractType,
        @NotNull(message = "propertyId is required")
        @Positive(message = "propertyId must be positive")
        Long propertyId,
        @NotNull(message = "customerId is required")
        @Positive(message = "customerId must be positive")
        Long customerId,
        @NotNull(message = "agentId is required")
        @Positive(message = "agentId must be positive")
        Long agentId,
        @Positive(message = "templateId must be positive")
        Long templateId,
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
    public ContractCreateRequest {
        code = normalizeCode(code);
        title = trim(title);
        currency = currency == null ? "VND" : normalizeCode(currency);
        terms = trimToNull(terms);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "expirationDate must be on or after effectiveDate")
    public boolean hasValidDateRange() {
        return effectiveDate == null || expirationDate == null
                || !expirationDate.isBefore(effectiveDate);
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
