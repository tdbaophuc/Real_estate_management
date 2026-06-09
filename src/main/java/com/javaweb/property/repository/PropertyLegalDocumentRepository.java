package com.javaweb.property.repository;

import com.javaweb.property.entity.PropertyLegalDocument;
import com.javaweb.property.enums.DocumentVerificationStatus;
import com.javaweb.property.enums.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyLegalDocumentRepository extends JpaRepository<PropertyLegalDocument, Long> {
    List<PropertyLegalDocument> findAllByPropertyIdOrderByCreatedAtDesc(Long propertyId);

    List<PropertyLegalDocument> findAllByPropertyIdAndDocumentType(
            Long propertyId,
            LegalDocumentType documentType
    );

    List<PropertyLegalDocument> findAllByVerificationStatus(
            DocumentVerificationStatus verificationStatus
    );

    Optional<PropertyLegalDocument> findByStorageKey(String storageKey);
}
