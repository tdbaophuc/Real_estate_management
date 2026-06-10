package com.javaweb.transaction.dto;

import com.javaweb.payment.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreateRequest(
        @Positive Long paymentScheduleId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String currency,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 100) String referenceNumber,
        @NotBlank @Size(max = 100) String idempotencyKey,
        Instant paidAt,
        @Size(max = 1000) String notes
) {
    public PaymentCreateRequest {
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        referenceNumber = trimToNull(referenceNumber);
        idempotencyKey = idempotencyKey == null ? null : idempotencyKey.trim();
        notes = trimToNull(notes);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
