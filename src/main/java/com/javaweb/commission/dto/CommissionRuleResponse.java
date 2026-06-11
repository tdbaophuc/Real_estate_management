package com.javaweb.commission.dto;

import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.contract.enums.ContractType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CommissionRuleResponse(
        Long id,
        String code,
        String name,
        ContractType transactionType,
        CommissionCalculationType calculationType,
        BigDecimal rate,
        BigDecimal fixedAmount,
        String currency,
        BigDecimal minTransactionValue,
        BigDecimal maxTransactionValue,
        int priority,
        boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String description,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
}
