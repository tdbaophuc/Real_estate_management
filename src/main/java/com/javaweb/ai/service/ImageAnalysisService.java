package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.ImageAnalysisItemResponse;
import com.javaweb.ai.dto.ImageAnalysisRequest;
import com.javaweb.ai.dto.ImageAnalysisResponse;
import com.javaweb.ai.entity.AiImageAnalysis;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.provider.AiImageAnalysisProvider;
import com.javaweb.ai.provider.NoopImageAnalysisProvider;
import com.javaweb.ai.repository.AiImageAnalysisRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyImage;
import com.javaweb.property.repository.PropertyImageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageAnalysisService {
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final List<AiImageAnalysisProvider> providers;
    private final AiImageAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;

    public ImageAnalysisService(
            PropertyImageRepository propertyImageRepository,
            UserRepository userRepository,
            List<AiImageAnalysisProvider> providers,
            AiImageAnalysisRepository analysisRepository,
            ObjectMapper objectMapper
    ) {
        this.propertyImageRepository = propertyImageRepository;
        this.userRepository = userRepository;
        this.providers = providers;
        this.analysisRepository = analysisRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImageAnalysisResponse analyze(
            ImageAnalysisRequest request,
            AuthUserPrincipal actor
    ) {
        User generatedBy = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        List<PropertyImage> images = request.imageIds().stream()
                .map(imageId -> requireAccessibleImage(imageId, actor))
                .toList();
        AiImageAnalysisProvider provider = resolveProvider();
        boolean fallbackUsed = !provider.available();
        List<ImageAnalysisItemResponse> results = fallbackUsed
                ? fallback(images)
                : provider.analyze(images);
        AiRequestStatus status = fallbackUsed ? AiRequestStatus.SKIPPED : AiRequestStatus.SUCCESS;
        String errorMessage = fallbackUsed
                ? "Vision provider is not configured; returned metadata-based fallback analysis"
                : null;
        saveAnalyses(images, generatedBy, results, fallbackUsed, status, provider, errorMessage);
        return new ImageAnalysisResponse(
                fallbackUsed,
                status,
                provider.name(),
                provider.name(),
                errorMessage,
                results
        );
    }

    private PropertyImage requireAccessibleImage(Long imageId, AuthUserPrincipal actor) {
        PropertyImage image = propertyImageRepository.findWithAnalysisDetailsById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found"));
        Property property = image.getProperty();
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property image not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = property.getAssignedAgent() != null
                    && property.getAssignedAgent().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only analyze images for properties they created or are assigned"
                );
            }
        }
        return image;
    }

    private AiImageAnalysisProvider resolveProvider() {
        return providers.stream()
                .filter(AiImageAnalysisProvider::available)
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(provider -> NoopImageAnalysisProvider.PROVIDER_NAME.equals(provider.name()))
                        .findFirst()
                        .orElse(providers.getFirst()));
    }

    private List<ImageAnalysisItemResponse> fallback(List<PropertyImage> images) {
        List<ImageAnalysisItemResponse> results = new ArrayList<>();
        boolean coverAssigned = images.stream().anyMatch(PropertyImage::isCoverImage);
        for (PropertyImage image : images) {
            List<String> issues = new ArrayList<>();
            if (image.getMimeType() == null || !image.getMimeType().startsWith("image/")) {
                issues.add("File metadata is not an image MIME type");
            }
            if (image.getFileSize() != null && image.getFileSize() < 50_000) {
                issues.add("File is small; check resolution before publishing");
            }
            if (image.getAltText() == null || image.getAltText().isBlank()) {
                issues.add("Missing alt text or caption");
            }
            boolean suggestedCover = image.isCoverImage() || (!coverAssigned && results.isEmpty());
            results.add(new ImageAnalysisItemResponse(
                    image.getId(),
                    image.getImageUrl(),
                    false,
                    false,
                    false,
                    false,
                    suggestedCover,
                    image.getAltText() == null ? "Property image pending AI caption" : image.getAltText(),
                    issues,
                    issues.isEmpty()
                            ? "Metadata looks usable; run real vision analysis when configured"
                            : "Review image metadata and replace or enrich this image if needed"
            ));
        }
        return results;
    }

    private void saveAnalyses(
            List<PropertyImage> images,
            User generatedBy,
            List<ImageAnalysisItemResponse> results,
            boolean fallbackUsed,
            AiRequestStatus status,
            AiImageAnalysisProvider provider,
            String errorMessage
    ) {
        for (ImageAnalysisItemResponse result : results) {
            PropertyImage image = images.stream()
                    .filter(candidate -> candidate.getId().equals(result.imageId()))
                    .findFirst()
                    .orElseThrow();
            analysisRepository.save(new AiImageAnalysis(
                    image,
                    generatedBy,
                    toJson(result),
                    fallbackUsed,
                    status,
                    provider.name(),
                    provider.name(),
                    errorMessage
            ));
        }
    }

    private String toJson(ImageAnalysisItemResponse result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize image analysis result", exception);
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }
}
