package com.javaweb.property.dto;

import com.javaweb.property.enums.DocumentVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyLegalDocumentVerifyRequest(
        @NotNull(message = "verificationStatus is required")
        DocumentVerificationStatus verificationStatus,

        @Size(max = 1000, message = "notes must not exceed 1000 characters")
        String notes
) {
    public PropertyLegalDocumentVerifyRequest {
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            }
        }
    }
}
