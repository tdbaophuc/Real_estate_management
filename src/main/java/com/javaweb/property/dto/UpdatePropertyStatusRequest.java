package com.javaweb.property.dto;

import com.javaweb.property.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyStatusRequest(
        @NotNull(message = "status is required")
        PropertyStatus status
) {
}
