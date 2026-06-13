package com.javaweb.ai.dto;

import java.util.List;

public record ImageAnalysisItemResponse(
        Long imageId,
        String imageUrl,
        boolean blurry,
        boolean dark,
        boolean duplicateSuspected,
        boolean irrelevant,
        boolean suggestedCover,
        String caption,
        List<String> issues,
        String recommendation
) {
}
