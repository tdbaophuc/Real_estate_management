package com.javaweb.listing.dto;

import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;

import java.math.BigDecimal;

public record InternalListingPropertySummary(
        Long id,
        String code,
        String name,
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
        Long createdById,
        String createdByName,
        Long assignedAgentId,
        String assignedAgentName,
        String fullAddress
) {
}
