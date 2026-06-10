package com.javaweb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(max = 500, message = "refreshToken must not exceed 500 characters")
        String refreshToken
) {
}
