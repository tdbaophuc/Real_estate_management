package com.javaweb.customer.dto;

import jakarta.validation.constraints.NotNull;

public record CustomerNotePinRequest(
        @NotNull(message = "pinned is required")
        Boolean pinned
) {
}
