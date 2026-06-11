package com.javaweb.dashboard.dto;

import java.util.List;

public record AgentDashboardResponse(
        long myLeads,
        List<StatusCountResponse> myLeadsByStatus,
        long todayAppointments,
        long followUpTasks,
        long overdueFollowUpTasks,
        long activeTransactions,
        List<StatusCountResponse> activeTransactionsByStatus,
        long myCommissions,
        List<StatusCountResponse> myCommissionsByStatus,
        List<CurrencyAmountResponse> myCommissionAmounts
) {
}
