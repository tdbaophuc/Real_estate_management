package com.javaweb.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerTagRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "color must be a hex color like #D92D20"
        )
        String color
) {
    public CustomerTagRequest {
        name = name == null ? null : name.trim();
        color = color == null || color.isBlank() ? null : color.trim();
    }
}
