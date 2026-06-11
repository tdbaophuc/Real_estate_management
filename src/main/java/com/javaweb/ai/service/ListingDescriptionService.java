package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.ListingDescriptionRequest;
import com.javaweb.ai.dto.ListingDescriptionResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Property;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ListingDescriptionService {
    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final AiService aiService;
    private final ListingDescriptionPromptBuilder promptBuilder;
    private final ListingDescriptionFallbackBuilder fallbackBuilder;
    private final ObjectMapper objectMapper;

    public ListingDescriptionService(
            PropertyRepository propertyRepository,
            ListingRepository listingRepository,
            AiService aiService,
            ListingDescriptionPromptBuilder promptBuilder,
            ListingDescriptionFallbackBuilder fallbackBuilder,
            ObjectMapper objectMapper
    ) {
        this.propertyRepository = propertyRepository;
        this.listingRepository = listingRepository;
        this.aiService = aiService;
        this.promptBuilder = promptBuilder;
        this.fallbackBuilder = fallbackBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ListingDescriptionResponse generate(
            ListingDescriptionRequest request,
            AuthUserPrincipal actor
    ) {
        Listing listing = loadListing(request.listingId());
        Property property = listing == null ? loadProperty(request.propertyId()) : listing.getProperty();
        requireActive(property);
        requireCanGenerate(property, listing, actor);

        ListingDescriptionPrompt prompt = promptBuilder.build(property, listing, request);
        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                ListingDescriptionPromptBuilder.OPERATION,
                prompt.systemPrompt(),
                prompt.userPrompt(),
                prompt.referenceType(),
                prompt.referenceId(),
                prompt.metadataJson()
        ));

        if (aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())) {
            try {
                return parse(aiResponse).toResponse(
                        false,
                        aiResponse.status(),
                        aiResponse.provider(),
                        aiResponse.model(),
                        null
                );
            } catch (IllegalArgumentException exception) {
                return fallback(property, listing, request, aiResponse, exception.getMessage());
            }
        }

        return fallback(property, listing, request, aiResponse, aiResponse.errorMessage());
    }

    private Listing loadListing(Long listingId) {
        if (listingId == null) {
            return null;
        }
        Listing listing = listingRepository.findWithAiDescriptionDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing not found");
        }
        return listing;
    }

    private Property loadProperty(Long propertyId) {
        return propertyRepository.findActiveDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
    }

    private void requireActive(Property property) {
        if (property.getDeletedAt() != null || property.getStatus() == PropertyStatus.DELETED) {
            throw new ResourceNotFoundException("Property not found");
        }
    }

    private void requireCanGenerate(Property property, Listing listing, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (listing != null && listing.getCreatedBy().getId().equals(actor.id())) {
            return;
        }
        boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
        boolean assignedToActor = property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!createdByActor && !assignedToActor) {
            throw new AccessDeniedException(
                    "Agents can only generate listing descriptions for assigned properties or own listings"
            );
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private ListingDescriptionDraft parse(AiCompletionResponse response) {
        try {
            JsonNode root = objectMapper.readTree(response.content());
            String title = requiredText(root, "title", 250);
            String shortDescription = requiredText(root, "shortDescription", 500);
            String fullDescription = requiredText(root, "fullDescription", 4000);
            String socialMediaCaption = optionalText(root, "socialMediaCaption", 500);
            List<String> seoKeywords = seoKeywords(root.get("seoKeywords"));
            return new ListingDescriptionDraft(
                    title,
                    shortDescription,
                    fullDescription,
                    seoKeywords,
                    socialMediaCaption
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid listing-description JSON");
        }
    }

    private String requiredText(JsonNode root, String field, int maxLength) {
        String value = optionalText(root, field, maxLength);
        if (!hasText(value)) {
            throw new BusinessException("AI response is missing " + field);
        }
        return value;
    }

    private String optionalText(JsonNode root, String field, int maxLength) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText().trim();
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    private List<String> seoKeywords(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new BusinessException("AI response is missing seoKeywords");
        }
        List<String> keywords = new ArrayList<>();
        for (JsonNode keyword : node) {
            String value = keyword.asText().trim();
            if (hasText(value) && !keywords.contains(value)) {
                keywords.add(value.length() > 80 ? value.substring(0, 80) : value);
            }
        }
        if (keywords.isEmpty()) {
            throw new BusinessException("AI response is missing seoKeywords");
        }
        return keywords.stream().limit(10).toList();
    }

    private ListingDescriptionResponse fallback(
            Property property,
            Listing listing,
            ListingDescriptionRequest request,
            AiCompletionResponse aiResponse,
            String errorMessage
    ) {
        return fallbackBuilder.build(property, listing, request)
                .toResponse(
                        true,
                        aiResponse.status(),
                        aiResponse.provider(),
                        aiResponse.model(),
                        errorMessage
                );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
