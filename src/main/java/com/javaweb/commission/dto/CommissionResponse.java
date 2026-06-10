package com.javaweb.commission.dto;

import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.contract.enums.ContractType;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionResponse(
        Long id,
        Long transactionId,
        String transactionCode,
        ContractType transactionType,
        Long commissionRuleId,
        String commissionRuleCode,
        CommissionCalculationType calculationType,
        Long beneficiaryUserId,
        String beneficiaryName,
        CommissionStatus status,
        BigDecimal baseAmount,
        BigDecimal rate,
        BigDecimal amount,
        String currency,
        Long approvedById,
        String approvedByName,
        Instant approvedAt,
        Long paidById,
        String paidByName,
        Instant paidAt,
        String paymentReference,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
