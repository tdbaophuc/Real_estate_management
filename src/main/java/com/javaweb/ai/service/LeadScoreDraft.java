package com.javaweb.ai.service;

import com.javaweb.lead.enums.LeadPriority;

record LeadScoreDraft(
        int score,
        LeadPriority priority,
        String reason,
        String suggestedFollowUp
) {
}
