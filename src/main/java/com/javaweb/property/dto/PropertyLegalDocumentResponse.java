package com.javaweb.property.dto;

import com.javaweb.property.entity.PropertyLegalDocument;
import com.javaweb.property.enums.DocumentVerificationStatus;
import com.javaweb.property.enums.LegalDocumentType;

import java.time.Instant;
import java.time.LocalDate;

public record PropertyLegalDocumentResponse(
        Long id,
        LegalDocumentType documentType,
        String documentNumber,
        String issuedBy,
        LocalDate issuedDate,
        LocalDate expiryDate,
        DocumentVerificationStatus verificationStatus,
        String storageKey,
        String documentUrl,
        String fileName,
        String notes,
        Long uploadedById,
        String uploadedByName,
        Instant createdAt,
        Instant updatedAt
) {
    public static PropertyLegalDocumentResponse from(PropertyLegalDocument document) {
        return new PropertyLegalDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssuedBy(),
                document.getIssuedDate(),
                document.getExpiryDate(),
                document.getVerificationStatus(),
                document.getStorageKey(),
                document.getDocumentUrl(),
                document.getFileName(),
                document.getNotes(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getFullName(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
