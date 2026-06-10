package com.javaweb.property.dto;

import com.javaweb.property.enums.FurnitureStatus;
import com.javaweb.property.enums.PropertyDirection;
import com.javaweb.property.enums.PropertyLegalStatus;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PropertyResponse(
        Long id,
        String code,
        String name,
        String description,
        Long propertyTypeId,
        String propertyTypeCode,
        String propertyTypeName,
        PropertyPurpose purpose,
        PropertyStatus status,
        BigDecimal price,
        String currency,
        BigDecimal landArea,
        BigDecimal floorArea,
        Integer bedrooms,
        Integer bathrooms,
        Integer floors,
        PropertyDirection direction,
        PropertyLegalStatus legalStatus,
        FurnitureStatus furnitureStatus,
        String videoUrl,
        String virtualTourUrl,
        LocalDate availableFrom,
        Long ownerId,
        String ownerName,
        Long createdById,
        String createdByName,
        Long assignedAgentId,
        String assignedAgentName,
        PropertyAddressResponse address,
        List<PropertyAmenityResponse> amenities,
        Instant createdAt,
        Instant updatedAt
) {
}
