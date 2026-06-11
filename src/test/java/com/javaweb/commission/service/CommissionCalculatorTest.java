package com.javaweb.commission.service;

import com.javaweb.auth.entity.User;
import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.transaction.entity.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommissionCalculatorTest {
    private final CommissionCalculator calculator = new CommissionCalculator();

    @Test
    void shouldCalculatePercentageAndRoundToCurrencyScale() {
        CommissionRule rule = rule(
                "SALE-2.5",
                CommissionCalculationType.PERCENTAGE,
                10
        );
        rule.setRate(new BigDecimal("2.5000"));

        assertThat(calculator.calculate(rule, new BigDecimal("1000001")))
                .isEqualByComparingTo("25000.03");
    }

    @Test
    void shouldCalculateFixedAmount() {
        CommissionRule rule = rule(
                "FIXED-15K",
                CommissionCalculationType.FIXED,
                10
        );
        rule.setFixedAmount(new BigDecimal("15000"));

        assertThat(calculator.calculate(rule, new BigDecimal("1000000")))
                .isEqualByComparingTo("15000.00");
    }

    @Test
    void shouldSelectHighestPriorityMatchingRule() {
        LocalDate calculationDate = LocalDate.of(2026, 6, 10);
        Transaction transaction = transaction(
                ContractType.SALE,
                "VND",
                new BigDecimal("1000000")
        );
        CommissionRule expired = percentageRule("EXPIRED", 100, "9.0");
        expired.setEffectiveTo(calculationDate.minusDays(1));
        CommissionRule outOfRange = percentageRule("HIGH-VALUE", 90, "8.0");
        outOfRange.setMinTransactionValue(new BigDecimal("2000000"));
        CommissionRule generic = percentageRule("GENERIC", 20, "1.0");
        CommissionRule sale = percentageRule("SALE", 20, "2.0");
        sale.setTransactionType(ContractType.SALE);

        assertThat(calculator.selectRule(
                        List.of(expired, outOfRange, generic, sale),
                        transaction,
                        calculationDate
                ))
                .containsSame(sale);
    }

    @Test
    void shouldRejectInactiveWrongCurrencyAndWrongTypeRules() {
        Transaction transaction = transaction(
                ContractType.LEASE,
                "VND",
                new BigDecimal("500000")
        );
        CommissionRule inactive = percentageRule("INACTIVE", 30, "3.0");
        inactive.setActive(false);
        CommissionRule wrongCurrency = percentageRule("USD", 20, "2.0");
        wrongCurrency.setCurrency("USD");
        CommissionRule wrongType = percentageRule("SALE", 10, "1.0");
        wrongType.setTransactionType(ContractType.SALE);

        assertThat(calculator.selectRule(
                List.of(inactive, wrongCurrency, wrongType),
                transaction,
                LocalDate.of(2026, 6, 10)
        )).isEmpty();
    }

    private CommissionRule percentageRule(
            String code,
            int priority,
            String rate
    ) {
        CommissionRule rule = rule(
                code,
                CommissionCalculationType.PERCENTAGE,
                priority
        );
        rule.setRate(new BigDecimal(rate));
        return rule;
    }

    private CommissionRule rule(
            String code,
            CommissionCalculationType type,
            int priority
    ) {
        CommissionRule rule = new CommissionRule(
                code,
                code,
                type,
                mock(User.class)
        );
        rule.setPriority(priority);
        return rule;
    }

    private Transaction transaction(
            ContractType type,
            String currency,
            BigDecimal agreedValue
    ) {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getTransactionType()).thenReturn(type);
        when(transaction.getCurrency()).thenReturn(currency);
        when(transaction.getAgreedValue()).thenReturn(agreedValue);
        return transaction;
    }
}
