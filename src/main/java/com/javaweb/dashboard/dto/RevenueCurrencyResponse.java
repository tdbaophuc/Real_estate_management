package com.javaweb.dashboard.dto;

import java.math.BigDecimal;

public record RevenueCurrencyResponse(
        String currency,
        long completedTransactions,
        BigDecimal completedTransactionValue,
        BigDecimal completedPayments,
        BigDecimal verifiedDeposits,
        BigDecimal paidCommissions
) {
}
