package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.lead.enums.LeadPriority;

public record LeadScoreResponse(
        Long leadId,
        String leadCode,
        int score,
        LeadPriority priority,
        String reason,
        String suggestedFollowUp,
        boolean fallbackUsed,
        AiRequestStatus aiStatus,
        String provider,
        String model,
        String errorMessage
) {
}
