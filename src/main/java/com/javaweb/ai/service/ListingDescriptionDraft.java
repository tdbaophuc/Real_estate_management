package com.javaweb.ai.service;

import com.javaweb.ai.dto.ListingDescriptionResponse;
import com.javaweb.ai.enums.AiRequestStatus;

import java.util.List;

record ListingDescriptionDraft(
        String title,
        String shortDescription,
        String fullDescription,
        List<String> seoKeywords,
        String socialMediaCaption
) {
    ListingDescriptionResponse toResponse(
            boolean fallbackUsed,
            AiRequestStatus aiStatus,
            String provider,
            String model,
            String errorMessage
    ) {
        return new ListingDescriptionResponse(
                title,
                shortDescription,
                fullDescription,
                seoKeywords,
                socialMediaCaption,
                fallbackUsed,
                aiStatus,
                provider,
                model,
                errorMessage
        );
    }
}
