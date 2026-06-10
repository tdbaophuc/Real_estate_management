package com.javaweb.transaction.dto;

import com.javaweb.transaction.enums.PaymentScheduleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentScheduleResponse(
        Long id,
        int installmentNumber,
        String label,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        String currency,
        PaymentScheduleStatus status,
        Instant paidAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
