package com.javaweb.property.dto;

import com.javaweb.property.enums.LegalDocumentType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PropertyLegalDocumentUpdateRequest(
        LegalDocumentType documentType,

        @Size(max = 100, message = "documentNumber must not exceed 100 characters")
        String documentNumber,

        @Size(max = 200, message = "issuedBy must not exceed 200 characters")
        String issuedBy,

        LocalDate issuedDate,

        LocalDate expiryDate,

        @Size(max = 1000, message = "notes must not exceed 1000 characters")
        String notes
) {
    public PropertyLegalDocumentUpdateRequest {
        documentNumber = trimToNull(documentNumber);
        issuedBy = trimToNull(issuedBy);
        notes = trimToNull(notes);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
