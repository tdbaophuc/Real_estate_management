package com.javaweb.transaction.dto;

import com.javaweb.contract.enums.ContractType;
import com.javaweb.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TransactionResponse(
        Long id,
        String code,
        ContractType transactionType,
        TransactionStatus status,
        Long contractId,
        String contractCode,
        Long propertyId,
        String propertyCode,
        String propertyName,
        Long customerId,
        String customerCode,
        String customerName,
        Long ownerId,
        String ownerName,
        Long agentId,
        String agentName,
        Long createdById,
        String createdByName,
        BigDecimal agreedValue,
        BigDecimal confirmedAmount,
        BigDecimal remainingAmount,
        String currency,
        LocalDate transactionDate,
        LocalDate expectedCompletionDate,
        Instant completedAt,
        Instant cancelledAt,
        Instant refundedAt,
        String cancellationReason,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<DepositResponse> deposits,
        List<PaymentScheduleResponse> paymentSchedules,
        List<PaymentResponse> payments,
        List<InvoiceResponse> invoices
) {
}
