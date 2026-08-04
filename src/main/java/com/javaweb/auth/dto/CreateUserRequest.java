package com.javaweb.auth.dto;

import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 200, message = "password must contain between 12 and 200 characters")
        @StrongPassword
        String password,

        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must not exceed 150 characters")
        String fullName,

        @Pattern(
                regexp = "^\\+?[0-9]{8,15}$",
                message = "phone must contain between 8 and 15 digits"
        )
        String phone,

        @NotEmpty(message = "roles must contain at least one role")
        @Size(max = 5, message = "roles must not contain more than 5 roles")
        Set<@NotNull(message = "role must not be null") RoleCode> roles
) {
    public CreateUserRequest {
        email = email == null ? null : email.trim();
        fullName = fullName == null ? null : fullName.trim();
        phone = phone == null ? null : phone.trim();
        roles = roles == null ? null : Set.copyOf(roles);
    }
}
