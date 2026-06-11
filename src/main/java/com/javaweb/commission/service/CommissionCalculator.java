package com.javaweb.commission.service;

import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class CommissionCalculator {
    public Optional<CommissionRule> selectRule(
            List<CommissionRule> rules,
            Transaction transaction,
            LocalDate calculationDate
    ) {
        return rules.stream()
                .filter(rule -> matches(rule, transaction, calculationDate))
                .sorted(Comparator.comparingInt(CommissionRule::getPriority)
                        .reversed()
                        .thenComparing(
                                rule -> rule.getTransactionType() == null ? 1 : 0
                        )
                        .thenComparing(
                                CommissionRule::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .findFirst();
    }

    public BigDecimal calculate(CommissionRule rule, BigDecimal baseAmount) {
        if (rule.getCalculationType() == CommissionCalculationType.FIXED) {
            return rule.getFixedAmount().setScale(2, RoundingMode.HALF_UP);
        }
        return baseAmount
                .multiply(rule.getRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    boolean matches(
            CommissionRule rule,
            Transaction transaction,
            LocalDate calculationDate
    ) {
        if (!rule.isActive()
                || rule.getTransactionType() != null
                && rule.getTransactionType() != transaction.getTransactionType()
                || !rule.getCurrency().equals(transaction.getCurrency())) {
            return false;
        }
        BigDecimal value = transaction.getAgreedValue();
        if (rule.getMinTransactionValue() != null
                && value.compareTo(rule.getMinTransactionValue()) < 0) {
            return false;
        }
        if (rule.getMaxTransactionValue() != null
                && value.compareTo(rule.getMaxTransactionValue()) > 0) {
            return false;
        }
        return (rule.getEffectiveFrom() == null
                || !calculationDate.isBefore(rule.getEffectiveFrom()))
                && (rule.getEffectiveTo() == null
                || !calculationDate.isAfter(rule.getEffectiveTo()));
    }
}
