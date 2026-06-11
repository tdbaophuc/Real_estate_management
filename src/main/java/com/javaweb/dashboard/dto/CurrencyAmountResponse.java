package com.javaweb.dashboard.dto;

import java.math.BigDecimal;

public record CurrencyAmountResponse(
        String currency,
        long count,
        BigDecimal amount
) {
}
