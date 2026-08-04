package com.javaweb.masterdata.dto;

public record LocationOptionResponse(
        Long id,
        String code,
        String name,
        String administrativeType,
        boolean active
) {
}
