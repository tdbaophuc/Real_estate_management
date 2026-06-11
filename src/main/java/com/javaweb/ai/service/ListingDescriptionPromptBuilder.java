package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.ListingDescriptionRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyAmenity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ListingDescriptionPromptBuilder {
    static final String OPERATION = "LISTING_DESCRIPTION";

    private static final String SYSTEM_PROMPT = """
            You are a Vietnamese real-estate copywriter. Write accurate, compliant listing copy.
            Do not invent facts that are not present in the input. Return only valid JSON with keys:
            title, shortDescription, fullDescription, seoKeywords, socialMediaCaption.
            seoKeywords must be an array of 5 to 10 concise keyword strings.
            """;

    private final ObjectMapper objectMapper;

    public ListingDescriptionPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ListingDescriptionPrompt build(Property property, Listing listing, ListingDescriptionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language", request.language() == null ? "vi" : request.language());
        payload.put("tone", request.tone() == null ? "professional" : request.tone());
        payload.put("sellingPoints", request.sellingPoints());
        payload.put("property", propertyPayload(property));
        if (listing != null) {
            payload.put("listing", listingPayload(listing));
        }

        String sourceJson = toJson(payload);
        String referenceType = listing == null ? "property" : "listing";
        Long referenceId = listing == null ? property.getId() : listing.getId();
        return new ListingDescriptionPrompt(
                SYSTEM_PROMPT,
                """
                        Create listing marketing copy from this source data.
                        Respect the requested JSON schema exactly and keep descriptions concise.
                        Source data:
                        %s
                        """.formatted(sourceJson),
                sourceJson,
                referenceType,
                referenceId
        );
    }

    private Map<String, Object> propertyPayload(Property property) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", property.getCode());
        payload.put("name", property.getName());
        payload.put("description", property.getDescription());
        payload.put("propertyType", property.getPropertyType().getName());
        payload.put("purpose", enumName(property.getPurpose()));
        payload.put("addressSummary", addressSummary(property.getAddress()));
        payload.put("price", money(property.getPrice(), property.getCurrency()));
        payload.put("landArea", area(property.getLandArea()));
        payload.put("floorArea", area(property.getFloorArea()));
        payload.put("bedrooms", property.getBedrooms());
        payload.put("bathrooms", property.getBathrooms());
        payload.put("floors", property.getFloors());
        payload.put("direction", enumName(property.getDirection()));
        payload.put("legalStatus", enumName(property.getLegalStatus()));
        payload.put("furnitureStatus", enumName(property.getFurnitureStatus()));
        payload.put("amenities", amenities(property));
        return payload;
    }

    private Map<String, Object> listingPayload(Listing listing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", listing.getCode());
        payload.put("currentTitle", listing.getTitle());
        payload.put("currentDescription", listing.getDescription());
        payload.put("purpose", enumName(listing.getPurpose()));
        payload.put("askingPrice", money(listing.getAskingPrice(), listing.getCurrency()));
        payload.put("seoTitle", listing.getSeoTitle());
        payload.put("seoDescription", listing.getSeoDescription());
        payload.put("seoKeywords", listing.getSeoKeywords());
        return payload;
    }

    private String addressSummary(Address address) {
        if (address.getFullAddress() != null && !address.getFullAddress().isBlank()) {
            return address.getFullAddress();
        }
        return List.of(
                        address.getStreetAddress(),
                        address.getWard() == null ? null : address.getWard().getName(),
                        address.getDistrict() == null ? null : address.getDistrict().getName(),
                        address.getProvince() == null ? null : address.getProvince().getName()
                ).stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private List<String> amenities(Property property) {
        return property.getAmenities().stream()
                .map(this::amenityName)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String amenityName(PropertyAmenity propertyAmenity) {
        if (propertyAmenity.getDetails() != null && !propertyAmenity.getDetails().isBlank()) {
            return propertyAmenity.getAmenity().getName() + " - " + propertyAmenity.getDetails();
        }
        return propertyAmenity.getAmenity().getName();
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        return formatter.format(amount) + " " + (currency == null ? "VND" : currency);
    }

    private String area(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString() + " m2";
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI listing description prompt", exception);
        }
    }
}
