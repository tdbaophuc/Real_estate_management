package com.javaweb.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record ManagerDashboardResponse(
        long totalAgents,
        long totalLeads,
        List<StatusCountResponse> leadsByStatus,
        BigDecimal leadCloseRate,
        long totalTransactions,
        List<StatusCountResponse> transactionsByStatus,
        long pendingCommissions,
        long paidCommissions,
        List<RevenueCurrencyResponse> revenueSummary,
        List<TopAgentResponse> topAgents
) {
}
