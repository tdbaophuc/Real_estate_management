package com.javaweb.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record CommissionReportResponse(
        LocalDate from,
        LocalDate to,
        long totalCommissions,
        List<StatusCountResponse> commissionsByStatus,
        List<CurrencyAmountResponse> commissionAmounts
) {
}
