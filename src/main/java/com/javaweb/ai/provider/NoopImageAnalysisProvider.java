package com.javaweb.ai.provider;

import com.javaweb.ai.dto.ImageAnalysisItemResponse;
import com.javaweb.property.entity.PropertyImage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoopImageAnalysisProvider implements AiImageAnalysisProvider {
    public static final String PROVIDER_NAME = "noop-vision";

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<ImageAnalysisItemResponse> analyze(List<PropertyImage> images) {
        return List.of();
    }
}
