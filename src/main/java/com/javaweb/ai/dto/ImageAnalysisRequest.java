package com.javaweb.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImageAnalysisRequest(
        @NotEmpty(message = "imageIds is required")
        @Size(max = 20, message = "imageIds must not contain more than 20 items")
        List<Long> imageIds
) {
    public ImageAnalysisRequest {
        imageIds = imageIds == null ? List.of() : imageIds.stream().distinct().toList();
    }
}
