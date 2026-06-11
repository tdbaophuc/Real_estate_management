package com.javaweb.lead.dto;

import com.javaweb.lead.enums.LeadPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LeadCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
                message = "code may only contain letters, digits, underscores and hyphens"
        )
        String code,
        @NotBlank(message = "sourceCode is required")
        @Size(max = 50, message = "sourceCode must not exceed 50 characters")
        String sourceCode,
        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must not exceed 150 characters")
        String fullName,
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,
        @Size(max = 30, message = "phone must not exceed 30 characters")
        String phone,
        @NotNull(message = "priority is required")
        LeadPriority priority,
        @Size(max = 4000, message = "message must not exceed 4000 characters")
        String message,
        @Positive(message = "customerId must be positive")
        Long customerId,
        @Positive(message = "listingId must be positive")
        Long listingId,
        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId
) {
    public LeadCreateRequest {
        code = normalizeCode(code);
        sourceCode = normalizeCode(sourceCode);
        fullName = trim(fullName);
        email = normalizeEmail(email);
        phone = trimToNull(phone);
        message = trimToNull(message);
    }

    @AssertTrue(message = "email, phone or customerId is required")
    public boolean hasContact() {
        return email != null || phone != null || customerId != null;
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
