package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiRequestStatus;

import java.util.List;

public record ListingDescriptionResponse(
        String title,
        String shortDescription,
        String fullDescription,
        List<String> seoKeywords,
        String socialMediaCaption,
        boolean fallbackUsed,
        AiRequestStatus aiStatus,
        String provider,
        String model,
        String errorMessage
) {
}
