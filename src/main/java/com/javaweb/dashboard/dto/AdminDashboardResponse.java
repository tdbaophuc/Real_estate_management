package com.javaweb.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalProperties,
        long totalListings,
        long pendingListings,
        long totalLeads,
        List<StatusCountResponse> leadsByStatus,
        long totalTransactions,
        List<StatusCountResponse> transactionsByStatus,
        List<RevenueCurrencyResponse> revenueSummary,
        List<TopAgentResponse> topAgents
) {
}
