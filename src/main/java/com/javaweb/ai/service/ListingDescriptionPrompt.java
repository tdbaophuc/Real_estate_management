package com.javaweb.ai.service;

record ListingDescriptionPrompt(
        String systemPrompt,
        String userPrompt,
        String metadataJson,
        String referenceType,
        Long referenceId
) {
}
