package com.javaweb.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentScheduleCreateRequest(
        @Positive int installmentNumber,
        @NotBlank @Size(max = 200) String label,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String currency,
        @Size(max = 1000) String notes
) {
    public PaymentScheduleCreateRequest {
        label = label == null ? null : label.trim();
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
