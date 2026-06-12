package com.javaweb.ai.service;

import com.javaweb.ai.dto.PropertyRecommendationRequest;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
class PropertyRecommendationScorer {

    PropertyRecommendationDraft score(
            Listing listing,
            Customer customer,
            CustomerRequirement requirement,
            PropertyRecommendationRequest request
    ) {
        List<String> reasons = new ArrayList<>();
        int score = 35;
        Property property = listing.getProperty();

        score += matchPurpose(listing, requirement, reasons);
        score += matchPropertyType(property, requirement, reasons);
        score += matchLocation(property.getAddress(), requirement, reasons);
        score += matchBudget(listing, requirement, reasons);
        score += matchArea(property, requirement, reasons);
        score += matchRooms(property, requirement, reasons);
        score += matchNaturalNeed(listing, property, request, reasons);

        if (reasons.isEmpty()) {
            reasons.add("Listing is publicly available and broadly aligned with customer demand");
        }
        int boundedScore = Math.max(0, Math.min(100, score));
        return new PropertyRecommendationDraft(
                listing.getId(),
                boundedScore,
                String.join("; ", reasons),
                suggestedAction(customer, boundedScore)
        );
    }

    private int matchPurpose(
            Listing listing,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null || requirement.getPurpose() == null) {
            return 0;
        }
        if (listing.getPurpose() == requirement.getPurpose()) {
            reasons.add("Matches requested " + listing.getPurpose().name().toLowerCase(Locale.ROOT) + " purpose");
            return 12;
        }
        return -20;
    }

    private int matchPropertyType(
            Property property,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null || requirement.getPropertyType() == null) {
            return 0;
        }
        if (property.getPropertyType().getId().equals(requirement.getPropertyType().getId())) {
            reasons.add("Matches preferred property type");
            return 12;
        }
        return -12;
    }

    private int matchLocation(
            Address address,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null) {
            return 0;
        }
        if (requirement.getWard() != null && address.getWard() != null
                && address.getWard().getId().equals(requirement.getWard().getId())) {
            reasons.add("Matches preferred ward");
            return 20;
        }
        if (requirement.getDistrict() != null && address.getDistrict() != null
                && address.getDistrict().getId().equals(requirement.getDistrict().getId())) {
            reasons.add("Matches preferred district");
            return 16;
        }
        if (requirement.getProvince() != null
                && address.getProvince().getId().equals(requirement.getProvince().getId())) {
            reasons.add("Matches preferred province");
            return 10;
        }
        return 0;
    }

    private int matchBudget(
            Listing listing,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null || listing.getAskingPrice() == null) {
            return 0;
        }
        BigDecimal price = listing.getAskingPrice();
        BigDecimal min = requirement.getMinBudget();
        BigDecimal max = requirement.getMaxBudget();
        if ((min == null || price.compareTo(min) >= 0) && (max == null || price.compareTo(max) <= 0)) {
            reasons.add("Fits the requested budget range");
            return 18;
        }
        if (max != null && price.compareTo(max) > 0) {
            return -18;
        }
        return -6;
    }

    private int matchArea(
            Property property,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null) {
            return 0;
        }
        BigDecimal area = property.getFloorArea() == null ? property.getLandArea() : property.getFloorArea();
        if (area == null) {
            return 0;
        }
        BigDecimal min = requirement.getMinArea();
        BigDecimal max = requirement.getMaxArea();
        if ((min == null || area.compareTo(min) >= 0) && (max == null || area.compareTo(max) <= 0)) {
            reasons.add("Area fits the requested range");
            return 12;
        }
        return -8;
    }

    private int matchRooms(
            Property property,
            CustomerRequirement requirement,
            List<String> reasons
    ) {
        if (requirement == null) {
            return 0;
        }
        int score = 0;
        if (requirement.getMinBedrooms() != null && property.getBedrooms() != null
                && property.getBedrooms() >= requirement.getMinBedrooms()) {
            reasons.add("Bedroom count meets the requirement");
            score += 6;
        }
        if (requirement.getMinBathrooms() != null && property.getBathrooms() != null
                && property.getBathrooms() >= requirement.getMinBathrooms()) {
            reasons.add("Bathroom count meets the requirement");
            score += 4;
        }
        return score;
    }

    private int matchNaturalNeed(
            Listing listing,
            Property property,
            PropertyRecommendationRequest request,
            List<String> reasons
    ) {
        if (request.naturalLanguageNeed() == null) {
            return 0;
        }
        String haystack = String.join(
                " ",
                nullToEmpty(listing.getTitle()),
                nullToEmpty(listing.getDescription()),
                nullToEmpty(property.getName()),
                nullToEmpty(property.getDescription()),
                nullToEmpty(property.getAddress().getFullAddress())
        ).toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String token : request.naturalLanguageNeed().toLowerCase(Locale.ROOT).split("\\W+")) {
            if (token.length() >= 4 && haystack.contains(token)) {
                matches++;
            }
        }
        if (matches > 0) {
            reasons.add("Matches " + matches + " keyword(s) from the natural-language need");
        }
        return Math.min(10, matches * 2);
    }

    private String suggestedAction(Customer customer, int score) {
        if (score >= 85) {
            return "Prioritize this listing and schedule a viewing with " + customer.getFullName();
        }
        if (score >= 70) {
            return "Send this listing to the customer with a short personalized note";
        }
        return "Keep as a backup option and confirm missing preferences before pitching";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
