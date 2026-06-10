package com.javaweb.customer.dto;

import com.javaweb.customer.enums.CustomerPriority;
import com.javaweb.customer.enums.CustomerSource;
import com.javaweb.customer.enums.CustomerStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CustomerUpsertRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code may only contain letters, digits, underscores and hyphens"
        )
        String code,
        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must not exceed 150 characters")
        String fullName,
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,
        @Size(max = 30, message = "phone must not exceed 30 characters")
        String phone,
        @NotNull(message = "status is required")
        CustomerStatus status,
        @NotNull(message = "source is required")
        CustomerSource source,
        @NotNull(message = "priority is required")
        CustomerPriority priority,
        @Size(max = 30, message = "preferredContactMethod must not exceed 30 characters")
        String preferredContactMethod,
        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes,
        @Positive(message = "userId must be positive")
        Long userId,
        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId
) {
    public CustomerUpsertRequest {
        code = normalizeCode(code);
        fullName = trim(fullName);
        email = normalizeEmail(email);
        phone = trimToNull(phone);
        preferredContactMethod = trimToNull(preferredContactMethod);
        notes = trimToNull(notes);
    }

    @AssertTrue(message = "email, phone or userId is required")
    public boolean hasContact() {
        return email != null || phone != null || userId != null;
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
