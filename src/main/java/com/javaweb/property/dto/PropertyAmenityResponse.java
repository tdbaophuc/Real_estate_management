package com.javaweb.property.dto;

import com.javaweb.property.enums.AmenityCategory;

public record PropertyAmenityResponse(
        Long id,
        String code,
        String name,
        AmenityCategory category,
        String details
) {
}
