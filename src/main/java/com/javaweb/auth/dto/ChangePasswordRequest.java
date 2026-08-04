package com.javaweb.auth.dto;

import com.javaweb.auth.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,

        @NotBlank(message = "newPassword is required")
        @Size(min = 12, max = 200, message = "newPassword must contain between 12 and 200 characters")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
