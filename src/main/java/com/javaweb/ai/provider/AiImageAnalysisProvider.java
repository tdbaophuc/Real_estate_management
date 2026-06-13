package com.javaweb.ai.provider;

import com.javaweb.ai.dto.ImageAnalysisItemResponse;
import com.javaweb.property.entity.PropertyImage;

import java.util.List;

public interface AiImageAnalysisProvider {
    String name();

    boolean available();

    List<ImageAnalysisItemResponse> analyze(List<PropertyImage> images);
}
