package com.javaweb.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptResponse(
        Long id,
        String receiptNumber,
        Instant issuedAt,
        BigDecimal amount,
        String currency,
        String payerName,
        Long issuedById,
        String issuedByName,
        String notes,
        Instant createdAt
) {
}
