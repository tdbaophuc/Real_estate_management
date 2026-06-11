package com.javaweb.ai.service;

import com.javaweb.ai.dto.ListingDescriptionRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
class ListingDescriptionFallbackBuilder {

    ListingDescriptionDraft build(Property property, Listing listing, ListingDescriptionRequest request) {
        String propertyType = property.getPropertyType().getName();
        String location = location(property.getAddress());
        String purpose = listing == null ? property.getPurpose().name() : listing.getPurpose().name();
        String price = price(listing == null ? property.getPrice() : listing.getAskingPrice(),
                listing == null ? property.getCurrency() : listing.getCurrency());
        String area = area(property);
        String roomSummary = roomSummary(property);

        String title = firstPresent(
                listing == null ? null : listing.getTitle(),
                propertyType + " " + purpose.toLowerCase() + " tai " + location
        );
        String shortDescription = "%s %s, %s%s%s.".formatted(
                propertyType,
                purpose.toLowerCase(),
                location,
                price == null ? "" : ", gia " + price,
                area == null ? "" : ", dien tich " + area
        );

        List<String> highlights = new ArrayList<>();
        if (roomSummary != null) {
            highlights.add(roomSummary);
        }
        if (property.getLegalStatus() != null) {
            highlights.add("Phap ly: " + property.getLegalStatus().name());
        }
        if (property.getFurnitureStatus() != null) {
            highlights.add("Noi that: " + property.getFurnitureStatus().name());
        }
        highlights.addAll(request.sellingPoints());

        String fullDescription = shortDescription + " "
                + "Bat dong san phu hop cho khach hang can thong tin ro rang, vi tri de tiep can"
                + (highlights.isEmpty() ? "." : ". Diem noi bat: " + String.join("; ", highlights) + ".");

        List<String> seoKeywords = seoKeywords(propertyType, location, purpose, request.sellingPoints());
        String caption = title + " - " + shortDescription;
        return new ListingDescriptionDraft(
                limit(title, 250),
                limit(shortDescription, 500),
                fullDescription,
                seoKeywords,
                limit(caption, 500)
        );
    }

    private List<String> seoKeywords(
            String propertyType,
            String location,
            String purpose,
            List<String> sellingPoints
    ) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.add(propertyType);
        keywords.add(propertyType + " " + purpose.toLowerCase());
        keywords.add("bat dong san " + location);
        keywords.add("nha dat " + location);
        keywords.addAll(sellingPoints);
        return keywords.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(10)
                .toList();
    }

    private String roomSummary(Property property) {
        List<String> parts = new ArrayList<>();
        if (property.getBedrooms() != null) {
            parts.add(property.getBedrooms() + " phong ngu");
        }
        if (property.getBathrooms() != null) {
            parts.add(property.getBathrooms() + " phong tam");
        }
        if (property.getFloors() != null) {
            parts.add(property.getFloors() + " tang");
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String area(Property property) {
        BigDecimal area = property.getFloorArea() == null ? property.getLandArea() : property.getFloorArea();
        return area == null ? null : area.stripTrailingZeros().toPlainString() + " m2";
    }

    private String price(BigDecimal price, String currency) {
        if (price == null) {
            return null;
        }
        return price.stripTrailingZeros().toPlainString() + " " + (currency == null ? "VND" : currency);
    }

    private String location(Address address) {
        if (address.getDistrict() != null) {
            return address.getDistrict().getName();
        }
        if (address.getProvince() != null) {
            return address.getProvince().getName();
        }
        return address.getStreetAddress();
    }

    private String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
