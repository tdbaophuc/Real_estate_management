package com.javaweb.dashboard.dto;

public record TopAgentResponse(
        Long agentId,
        String agentName,
        long totalTransactions,
        long completedTransactions
) {
}
