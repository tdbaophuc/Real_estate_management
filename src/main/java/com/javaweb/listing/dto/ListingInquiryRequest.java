package com.javaweb.listing.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListingInquiryRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName must not exceed 150 characters")
        String fullName,
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,
        @Size(max = 30, message = "phone must not exceed 30 characters")
        String phone,
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must not exceed 4000 characters")
        String message,
        @Size(max = 30, message = "preferredContactMethod must not exceed 30 characters")
        String preferredContactMethod
) {
    public ListingInquiryRequest {
        fullName = trim(fullName);
        email = trimToNull(email);
        phone = trimToNull(phone);
        message = trim(message);
        preferredContactMethod = trimToNull(preferredContactMethod);
    }

    @AssertTrue(message = "email or phone is required")
    public boolean hasContact() {
        return email != null || phone != null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
