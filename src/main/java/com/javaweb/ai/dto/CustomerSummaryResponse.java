package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiRequestStatus;

import java.util.List;

public record CustomerSummaryResponse(
        Long customerId,
        String customerCode,
        String needsSummary,
        String interactionSummary,
        List<String> interestedProperties,
        String potentialLevel,
        String nextBestAction,
        boolean fallbackUsed,
        AiRequestStatus aiStatus,
        String provider,
        String model,
        String errorMessage
) {
}
