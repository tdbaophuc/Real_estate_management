package com.javaweb.masterdata.dto;

public record LeadSourceOptionResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active
) {
}
