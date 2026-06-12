package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.PropertyRecommendationRequest;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class PropertyRecommendationPromptBuilder {
    static final String OPERATION = "PROPERTY_RECOMMENDATION";

    private static final String SYSTEM_PROMPT = """
            You are a real-estate recommendation assistant. Rank only the candidate listings provided.
            Do not invent listings or facts. Return only valid JSON with key recommendations.
            recommendations must be an array of objects with keys listingId, matchScore, reason, suggestedAction.
            matchScore must be an integer from 0 to 100.
            """;

    private final ObjectMapper objectMapper;

    PropertyRecommendationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    PropertyRecommendationPrompt build(
            Customer customer,
            CustomerRequirement requirement,
            List<Listing> candidates,
            List<PropertyRecommendationDraft> fallbackScores,
            PropertyRecommendationRequest request
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language", request.language() == null ? "vi" : request.language());
        payload.put("maxResults", request.maxResults());
        payload.put("naturalLanguageNeed", request.naturalLanguageNeed());
        payload.put("customer", customerPayload(customer));
        payload.put("requirement", requirementPayload(requirement));
        payload.put("candidates", candidates.stream()
                .map(candidate -> candidatePayload(candidate, fallbackScores))
                .toList());

        String sourceJson = toJson(payload);
        return new PropertyRecommendationPrompt(
                SYSTEM_PROMPT,
                """
                        Rank these pre-filtered listings for the customer.
                        Keep reasons factual and concise, and suggest the next sales action.
                        Source data:
                        %s
                        """.formatted(sourceJson),
                sourceJson,
                "customer",
                customer.getId()
        );
    }

    private Map<String, Object> customerPayload(Customer customer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", customer.getId());
        payload.put("code", customer.getCode());
        payload.put("fullName", customer.getFullName());
        payload.put("priority", customer.getPriority().name());
        payload.put("notes", customer.getNotes());
        return payload;
    }

    private Map<String, Object> requirementPayload(CustomerRequirement requirement) {
        if (requirement == null) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("purpose", requirement.getPurpose().name());
        payload.put("propertyType", requirement.getPropertyType() == null
                ? null
                : requirement.getPropertyType().getName());
        payload.put("province", requirement.getProvince() == null ? null : requirement.getProvince().getName());
        payload.put("district", requirement.getDistrict() == null ? null : requirement.getDistrict().getName());
        payload.put("ward", requirement.getWard() == null ? null : requirement.getWard().getName());
        payload.put("budget", range(requirement.getMinBudget(), requirement.getMaxBudget(), requirement.getCurrency()));
        payload.put("area", range(requirement.getMinArea(), requirement.getMaxArea(), "m2"));
        payload.put("minBedrooms", requirement.getMinBedrooms());
        payload.put("minBathrooms", requirement.getMinBathrooms());
        payload.put("description", requirement.getDescription());
        return payload;
    }

    private Map<String, Object> candidatePayload(
            Listing listing,
            List<PropertyRecommendationDraft> fallbackScores
    ) {
        Property property = listing.getProperty();
        Address address = property.getAddress();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listingId", listing.getId());
        payload.put("title", listing.getTitle());
        payload.put("purpose", listing.getPurpose().name());
        payload.put("askingPrice", money(listing.getAskingPrice(), listing.getCurrency()));
        payload.put("propertyType", property.getPropertyType().getName());
        payload.put("propertyName", property.getName());
        payload.put("address", address.getFullAddress());
        payload.put("province", address.getProvince().getName());
        payload.put("district", address.getDistrict() == null ? null : address.getDistrict().getName());
        payload.put("ward", address.getWard() == null ? null : address.getWard().getName());
        payload.put("landArea", area(property.getLandArea()));
        payload.put("floorArea", area(property.getFloorArea()));
        payload.put("bedrooms", property.getBedrooms());
        payload.put("bathrooms", property.getBathrooms());
        payload.put("fallbackScore", fallbackScores.stream()
                .filter(score -> score.listingId().equals(listing.getId()))
                .findFirst()
                .map(PropertyRecommendationDraft::matchScore)
                .orElse(null));
        return payload;
    }

    private String range(BigDecimal min, BigDecimal max, String unit) {
        if (min == null && max == null) {
            return null;
        }
        String suffix = unit == null ? "" : " " + unit;
        if (min == null) {
            return "up to " + number(max) + suffix;
        }
        if (max == null) {
            return "from " + number(min) + suffix;
        }
        return number(min) + " - " + number(max) + suffix;
    }

    private String money(BigDecimal amount, String currency) {
        return amount == null ? null : number(amount) + " " + (currency == null ? "VND" : currency);
    }

    private String area(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString() + " m2";
    }

    private String number(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI property recommendation prompt", exception);
        }
    }
}
