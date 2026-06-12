package com.javaweb.ai.service;

record PropertyRecommendationPrompt(
        String systemPrompt,
        String userPrompt,
        String metadataJson,
        String referenceType,
        Long referenceId
) {
}
