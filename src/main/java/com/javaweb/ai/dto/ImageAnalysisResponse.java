package com.javaweb.ai.dto;

import com.javaweb.ai.enums.AiRequestStatus;

import java.util.List;

public record ImageAnalysisResponse(
        boolean fallbackUsed,
        AiRequestStatus aiStatus,
        String provider,
        String model,
        String errorMessage,
        List<ImageAnalysisItemResponse> images
) {
}
