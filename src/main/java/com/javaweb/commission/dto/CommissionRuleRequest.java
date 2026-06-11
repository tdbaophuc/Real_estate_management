package com.javaweb.commission.dto;

import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.contract.enums.ContractType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionRuleRequest(
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$")
        String code,
        @NotBlank @Size(max = 200) String name,
        ContractType transactionType,
        @NotNull CommissionCalculationType calculationType,
        @DecimalMin(value = "0.0001") @DecimalMax(value = "100")
        @Digits(integer = 3, fraction = 4)
        BigDecimal rate,
        @DecimalMin(value = "0.01")
        BigDecimal fixedAmount,
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency,
        @DecimalMin(value = "0")
        BigDecimal minTransactionValue,
        @DecimalMin(value = "0")
        BigDecimal maxTransactionValue,
        @Min(0) Integer priority,
        Boolean active,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Size(max = 1000) String description
) {
    public CommissionRuleRequest {
        code = code == null ? null : code.trim().toUpperCase();
        name = name == null ? null : name.trim();
        currency = currency == null ? "VND" : currency.trim().toUpperCase();
        priority = priority == null ? 0 : priority;
        active = active == null ? Boolean.TRUE : active;
        description = description == null || description.isBlank()
                ? null
                : description.trim();
    }

    @AssertTrue(message = "percentage rules require rate and no fixedAmount")
    public boolean hasValidPercentageValue() {
        return calculationType != CommissionCalculationType.PERCENTAGE
                || rate != null && fixedAmount == null;
    }

    @AssertTrue(message = "fixed rules require fixedAmount and no rate")
    public boolean hasValidFixedValue() {
        return calculationType != CommissionCalculationType.FIXED
                || fixedAmount != null && rate == null;
    }

    @AssertTrue(message = "maxTransactionValue must be at least minTransactionValue")
    public boolean hasValidValueRange() {
        return minTransactionValue == null
                || maxTransactionValue == null
                || maxTransactionValue.compareTo(minTransactionValue) >= 0;
    }

    @AssertTrue(message = "effectiveTo must not be before effectiveFrom")
    public boolean hasValidEffectiveDates() {
        return effectiveFrom == null
                || effectiveTo == null
                || !effectiveTo.isBefore(effectiveFrom);
    }
}
