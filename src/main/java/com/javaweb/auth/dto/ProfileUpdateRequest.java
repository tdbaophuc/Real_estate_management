package com.javaweb.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 150, message = "fullName must not exceed 150 characters")
        String fullName,

        @Pattern(
                regexp = "^\\+?[0-9]{8,15}$",
                message = "phone must contain between 8 and 15 digits"
        )
        String phone
) {
    public ProfileUpdateRequest {
        fullName = fullName == null ? null : fullName.trim();
        phone = phone == null ? null : phone.trim();
    }
}
