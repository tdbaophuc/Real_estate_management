package com.javaweb.transaction.dto;

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

public record TransactionCreateRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$")
        String code,
        @Positive Long contractId,
        @NotNull @Positive Long propertyId,
        @NotNull @Positive Long customerId,
        @NotNull @Positive Long agentId,
        @NotNull ContractType transactionType,
        @NotNull @DecimalMin(value = "0.0") BigDecimal agreedValue,
        @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
        LocalDate transactionDate,
        LocalDate expectedCompletionDate,
        @Size(max = 2000) String notes
) {
    public TransactionCreateRequest {
        code = code == null ? null : code.trim().toUpperCase();
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "expectedCompletionDate must be on or after transactionDate")
    public boolean hasValidDates() {
        return transactionDate == null || expectedCompletionDate == null
                || !expectedCompletionDate.isBefore(transactionDate);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
