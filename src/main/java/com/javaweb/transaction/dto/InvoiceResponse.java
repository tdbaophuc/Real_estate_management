package com.javaweb.transaction.dto;

import com.javaweb.payment.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        String billedToName,
        String billedToEmail,
        String billedToAddress,
        Long issuedById,
        String issuedByName,
        String notes,
        Instant createdAt
) {
}
