package com.javaweb.masterdata.dto;

import com.javaweb.property.enums.AmenityCategory;

public record AmenityOptionResponse(
        Long id,
        String code,
        String name,
        AmenityCategory category,
        String description,
        int displayOrder,
        boolean active
) {
}
