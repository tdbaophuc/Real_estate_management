package com.javaweb.transaction.dto;

import com.javaweb.payment.enums.PaymentMethod;
import com.javaweb.transaction.enums.DepositStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DepositResponse(
        Long id,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        DepositStatus status,
        String referenceNumber,
        String idempotencyKey,
        LocalDate dueDate,
        Instant receivedAt,
        Instant verifiedAt,
        Long receivedById,
        String receivedByName,
        String notes,
        Instant createdAt
) {
}
