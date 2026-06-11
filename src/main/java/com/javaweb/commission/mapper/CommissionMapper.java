package com.javaweb.commission.mapper;

import com.javaweb.commission.dto.CommissionResponse;
import com.javaweb.commission.dto.CommissionRuleResponse;
import com.javaweb.commission.entity.Commission;
import com.javaweb.commission.entity.CommissionRule;
import org.springframework.stereotype.Component;

@Component
public class CommissionMapper {
    public CommissionRuleResponse toRuleResponse(CommissionRule rule) {
        return new CommissionRuleResponse(
                rule.getId(),
                rule.getCode(),
                rule.getName(),
                rule.getTransactionType(),
                rule.getCalculationType(),
                rule.getRate(),
                rule.getFixedAmount(),
                rule.getCurrency(),
                rule.getMinTransactionValue(),
                rule.getMaxTransactionValue(),
                rule.getPriority(),
                rule.isActive(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo(),
                rule.getDescription(),
                rule.getCreatedBy().getId(),
                rule.getCreatedBy().getFullName(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    public CommissionResponse toResponse(Commission commission) {
        CommissionRule rule = commission.getCommissionRule();
        return new CommissionResponse(
                commission.getId(),
                commission.getTransaction().getId(),
                commission.getTransaction().getCode(),
                commission.getTransaction().getTransactionType(),
                rule == null ? null : rule.getId(),
                rule == null ? null : rule.getCode(),
                rule == null ? null : rule.getCalculationType(),
                commission.getBeneficiaryUser().getId(),
                commission.getBeneficiaryUser().getFullName(),
                commission.getStatus(),
                commission.getBaseAmount(),
                commission.getRate(),
                commission.getAmount(),
                commission.getCurrency(),
                commission.getApprovedBy() == null
                        ? null
                        : commission.getApprovedBy().getId(),
                commission.getApprovedBy() == null
                        ? null
                        : commission.getApprovedBy().getFullName(),
                commission.getApprovedAt(),
                commission.getPaidBy() == null
                        ? null
                        : commission.getPaidBy().getId(),
                commission.getPaidBy() == null
                        ? null
                        : commission.getPaidBy().getFullName(),
                commission.getPaidAt(),
                commission.getPaymentReference(),
                commission.getNotes(),
                commission.getCreatedAt(),
                commission.getUpdatedAt()
        );
    }
}
