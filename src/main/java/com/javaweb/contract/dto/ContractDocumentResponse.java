package com.javaweb.contract.dto;

import com.javaweb.contract.enums.ContractDocumentType;

import java.time.Instant;

public record ContractDocumentResponse(
        Long id,
        Long fileResourceId,
        String originalFileName,
        String contentType,
        long fileSize,
        ContractDocumentType documentType,
        int version,
        String displayName,
        String description,
        boolean primaryDocument,
        Long uploadedById,
        String uploadedByName,
        Instant createdAt
) {
}
