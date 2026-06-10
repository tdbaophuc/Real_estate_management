package com.javaweb.transaction.dto;

import com.javaweb.payment.enums.PaymentMethod;
import com.javaweb.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long paymentScheduleId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String referenceNumber,
        String idempotencyKey,
        Instant paidAt,
        Instant confirmedAt,
        Long receivedById,
        String receivedByName,
        String notes,
        ReceiptResponse receipt,
        Instant createdAt
) {
}
