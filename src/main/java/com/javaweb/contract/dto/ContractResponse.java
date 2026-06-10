package com.javaweb.contract.dto;

import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractResponse(
        Long id,
        String code,
        ContractType contractType,
        ContractStatus status,
        String title,
        Long templateId,
        Long propertyId,
        String propertyCode,
        String propertyName,
        Long customerId,
        String customerCode,
        String customerName,
        Long ownerId,
        String ownerName,
        Long agentId,
        String agentName,
        Long createdById,
        String createdByName,
        BigDecimal totalValue,
        String currency,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        String terms,
        String notes,
        Instant submittedAt,
        Instant approvedAt,
        Instant signedAt,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt,
        List<ContractPartyResponse> parties,
        List<ContractDocumentResponse> documents
) {
}
