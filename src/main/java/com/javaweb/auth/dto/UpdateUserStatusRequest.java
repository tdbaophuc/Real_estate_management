package com.javaweb.auth.dto;

import com.javaweb.auth.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "status is required")
        UserStatus status
) {
}
