package com.javaweb.ai.service;

record PropertyRecommendationDraft(
        Long listingId,
        int matchScore,
        String reason,
        String suggestedAction
) {
}
