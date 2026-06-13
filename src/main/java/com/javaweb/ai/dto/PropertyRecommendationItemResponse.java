package com.javaweb.ai.dto;

import com.javaweb.listing.dto.PublicListingResponse;

public record PropertyRecommendationItemResponse(
        PublicListingResponse listing,
        int matchScore,
        String reason,
        String suggestedAction
) {
}
