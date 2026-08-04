package com.javaweb.masterdata.dto;

public record PropertyTypeOptionResponse(
        Long id,
        String code,
        String name,
        String description,
        int displayOrder,
        boolean active
) {
}
