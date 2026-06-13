package com.javaweb.ai.service;

record LeadScorePrompt(
        String systemPrompt,
        String userPrompt,
        String metadataJson,
        String referenceType,
        Long referenceId
) {
}
