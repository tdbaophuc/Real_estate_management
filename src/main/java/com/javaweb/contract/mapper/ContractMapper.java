package com.javaweb.contract.mapper;

import com.javaweb.contract.dto.ContractDocumentResponse;
import com.javaweb.contract.dto.ContractPartyResponse;
import com.javaweb.contract.dto.ContractResponse;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.entity.ContractDocument;
import com.javaweb.contract.entity.ContractParty;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {
    public ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getCode(),
                contract.getContractType(),
                contract.getStatus(),
                contract.getTitle(),
                contract.getTemplate() == null ? null : contract.getTemplate().getId(),
                contract.getProperty().getId(),
                contract.getProperty().getCode(),
                contract.getProperty().getName(),
                contract.getCustomer().getId(),
                contract.getCustomer().getCode(),
                contract.getCustomer().getFullName(),
                contract.getOwner().getId(),
                contract.getOwner().getFullName(),
                contract.getAgent().getId(),
                contract.getAgent().getFullName(),
                contract.getCreatedBy().getId(),
                contract.getCreatedBy().getFullName(),
                contract.getTotalValue(),
                contract.getCurrency(),
                contract.getEffectiveDate(),
                contract.getExpirationDate(),
                contract.getTerms(),
                contract.getNotes(),
                contract.getSubmittedAt(),
                contract.getApprovedAt(),
                contract.getSignedAt(),
                contract.getCancelledAt(),
                contract.getCancellationReason(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getParties().stream()
                        .sorted((left, right) -> Integer.compare(
                                left.getSigningOrder(),
                                right.getSigningOrder()
                        ))
                        .map(this::toPartyResponse)
                        .toList(),
                contract.getDocuments().stream()
                        .sorted((left, right) -> right.getCreatedAt()
                                .compareTo(left.getCreatedAt()))
                        .map(this::toDocumentResponse)
                        .toList()
        );
    }

    public ContractDocumentResponse toDocumentResponse(ContractDocument document) {
        return new ContractDocumentResponse(
                document.getId(),
                document.getFileResource().getId(),
                document.getFileResource().getOriginalFileName(),
                document.getFileResource().getContentType(),
                document.getFileResource().getFileSize(),
                document.getDocumentType(),
                document.getVersion(),
                document.getDisplayName(),
                document.getDescription(),
                document.isPrimaryDocument(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getFullName(),
                document.getCreatedAt()
        );
    }

    private ContractPartyResponse toPartyResponse(ContractParty party) {
        return new ContractPartyResponse(
                party.getId(),
                party.getUser() == null ? null : party.getUser().getId(),
                party.getCustomer() == null ? null : party.getCustomer().getId(),
                party.getPartyRole(),
                party.getFullName(),
                party.getEmail(),
                party.getPhone(),
                party.getSigningOrder(),
                party.isRequiredSigner()
        );
    }
}
