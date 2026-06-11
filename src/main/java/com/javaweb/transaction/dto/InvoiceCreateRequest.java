package com.javaweb.transaction.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceCreateRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        @NotNull LocalDate issueDate,
        LocalDate dueDate,
        @NotNull @DecimalMin(value = "0.0") BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0.0") BigDecimal taxAmount,
        String currency,
        @NotBlank @Size(max = 200) String billedToName,
        @Size(max = 255) String billedToEmail,
        @Size(max = 500) String billedToAddress,
        @Size(max = 2000) String notes
) {
    public InvoiceCreateRequest {
        invoiceNumber = invoiceNumber == null ? null : invoiceNumber.trim().toUpperCase();
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        billedToName = billedToName == null ? null : billedToName.trim();
        billedToEmail = trimToNull(billedToEmail);
        billedToAddress = trimToNull(billedToAddress);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "dueDate must be on or after issueDate")
    public boolean hasValidDates() {
        return issueDate == null || dueDate == null || !dueDate.isBefore(issueDate);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
