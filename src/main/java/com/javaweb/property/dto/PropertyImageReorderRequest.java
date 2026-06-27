package com.javaweb.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;

import java.util.List;

public record PropertyImageReorderRequest(
        @NotEmpty(message = "items must not be empty")
        List<@Valid PropertyImageReorderItem> items
) {
    public PropertyImageReorderRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record PropertyImageReorderItem(
            @NotNull(message = "imageId is required")
            @Positive(message = "imageId must be positive")
            Long imageId,

            @NotNull(message = "displayOrder is required")
            @Min(value = 0, message = "displayOrder must be at least 0")
            Integer displayOrder
    ) {
    }
}
