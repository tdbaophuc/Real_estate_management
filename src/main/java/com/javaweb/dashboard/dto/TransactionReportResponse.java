package com.javaweb.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record TransactionReportResponse(
        LocalDate from,
        LocalDate to,
        long totalTransactions,
        List<StatusCountResponse> transactionsByStatus,
        List<CurrencyAmountResponse> completedTransactionValues
) {
}
