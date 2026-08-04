package com.javaweb.property.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.property.dto.PropertyLegalDocumentResponse;
import com.javaweb.property.dto.PropertyLegalDocumentUpdateRequest;
import com.javaweb.property.dto.PropertyLegalDocumentVerifyRequest;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyLegalDocument;
import com.javaweb.property.enums.DocumentVerificationStatus;
import com.javaweb.property.enums.LegalDocumentType;
import com.javaweb.property.repository.PropertyLegalDocumentRepository;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.repository.FileResourceRepository;
import com.javaweb.storage.service.FileResourceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PropertyLegalDocumentService {
    private final PropertyRepository propertyRepository;
    private final PropertyLegalDocumentRepository propertyLegalDocumentRepository;
    private final FileResourceService fileResourceService;
    private final FileResourceRepository fileResourceRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public PropertyLegalDocumentService(
            PropertyRepository propertyRepository,
            PropertyLegalDocumentRepository propertyLegalDocumentRepository,
            FileResourceService fileResourceService,
            FileResourceRepository fileResourceRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyLegalDocumentRepository = propertyLegalDocumentRepository;
        this.fileResourceService = fileResourceService;
        this.fileResourceRepository = fileResourceRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PropertyLegalDocumentResponse> list(Long propertyId, AuthUserPrincipal actor) {
        Property property = requireActiveProperty(propertyId);
        requireCanAccess(property, actor);
        return propertyLegalDocumentRepository.findAllByPropertyIdOrderByCreatedAtDesc(propertyId)
                .stream()
                .map(PropertyLegalDocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyLegalDocumentResponse get(
            Long propertyId,
            Long documentId,
            AuthUserPrincipal actor
    ) {
        Property property = requireActiveProperty(propertyId);
        requireCanAccess(property, actor);
        PropertyLegalDocument document = requireDocument(propertyId, documentId);
        return PropertyLegalDocumentResponse.from(document);
    }

    @Transactional
    public PropertyLegalDocumentResponse upload(
            Long propertyId,
            MultipartFile file,
            LegalDocumentType documentType,
            String documentNumber,
            String issuedBy,
            LocalDate issuedDate,
            LocalDate expiryDate,
            String notes,
            AuthUserPrincipal actor
    ) {
        Property property = requireActiveProperty(propertyId);
        requireCanAccess(property, actor);
        FileResource resource = fileResourceService.store(file, FileAccessLevel.PRIVATE, actor);
        try {
            PropertyLegalDocument document = new PropertyLegalDocument(
                    requireUser(actor.id()),
                    documentType
            );
            document.setDocumentNumber(trimToNull(documentNumber, 100));
            document.setIssuedBy(trimToNull(issuedBy, 200));
            document.setIssuedDate(issuedDate);
            document.setExpiryDate(expiryDate);
            document.setNotes(trimToNull(notes, 1000));
            document.setStorageKey(resource.getStorageKey());
            document.setDocumentUrl(resource.getPublicUrl());
            document.setFileName(resource.getOriginalFileName());
            property.addLegalDocument(document);
            PropertyLegalDocument saved = propertyLegalDocumentRepository.saveAndFlush(document);
            auditLogService.record(
                    actor,
                    AuditActions.PROPERTY_LEGAL_DOCUMENT_UPLOADED,
                    AuditActions.PROPERTY_LEGAL_DOCUMENT,
                    saved.getId(),
                    null,
                    toValueMap(saved)
            );
            return PropertyLegalDocumentResponse.from(saved);
        } catch (RuntimeException exception) {
            fileResourceService.delete(resource);
            throw exception;
        }
    }

    @Transactional
    public PropertyLegalDocumentResponse update(
            Long propertyId,
            Long documentId,
            PropertyLegalDocumentUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        Property property = requireActiveProperty(propertyId);
        requireCanAccess(property, actor);
        PropertyLegalDocument document = requireDocument(propertyId, documentId);
        Map<String, Object> oldValue = toValueMap(document);
        if (request.documentType() != null) {
            document.setDocumentType(request.documentType());
        }
        document.setDocumentNumber(trimToNull(request.documentNumber(), 100));
        document.setIssuedBy(trimToNull(request.issuedBy(), 200));
        document.setIssuedDate(request.issuedDate());
        document.setExpiryDate(request.expiryDate());
        document.setNotes(trimToNull(request.notes(), 1000));
        PropertyLegalDocument saved = propertyLegalDocumentRepository.saveAndFlush(document);
        auditLogService.record(
                actor,
                AuditActions.PROPERTY_LEGAL_DOCUMENT_UPDATED,
                AuditActions.PROPERTY_LEGAL_DOCUMENT,
                saved.getId(),
                oldValue,
                toValueMap(saved)
        );
        return PropertyLegalDocumentResponse.from(saved);
    }

    @Transactional
    public PropertyLegalDocumentResponse verify(
            Long propertyId,
            Long documentId,
            PropertyLegalDocumentVerifyRequest request,
            AuthUserPrincipal actor
    ) {
        requireManagerOrAdmin(actor);
        Property property = requireActiveProperty(propertyId);
        PropertyLegalDocument document = requireDocument(propertyId, documentId);
        if (request.verificationStatus() == DocumentVerificationStatus.UNVERIFIED) {
            throw new BusinessException("verificationStatus must not be UNVERIFIED");
        }
        Map<String, Object> oldValue = toValueMap(document);
        document.setVerificationStatus(request.verificationStatus());
        if (request.notes() != null) {
            document.setNotes(trimToNull(request.notes(), 1000));
        }
        PropertyLegalDocument saved = propertyLegalDocumentRepository.saveAndFlush(document);
        auditLogService.record(
                actor,
                AuditActions.PROPERTY_LEGAL_DOCUMENT_VERIFIED,
                AuditActions.PROPERTY_LEGAL_DOCUMENT,
                saved.getId(),
                oldValue,
                toValueMap(saved)
        );
        return PropertyLegalDocumentResponse.from(saved);
    }

    @Transactional
    public void delete(Long propertyId, Long documentId, AuthUserPrincipal actor) {
        requireManagerOrAdmin(actor);
        requireActiveProperty(propertyId);
        PropertyLegalDocument document = requireDocument(propertyId, documentId);
        Map<String, Object> oldValue = toValueMap(document);
        fileResourceRepository.findByStorageKey(document.getStorageKey())
                .ifPresent(fileResourceService::delete);
        propertyLegalDocumentRepository.delete(document);
        propertyLegalDocumentRepository.flush();
        auditLogService.record(
                actor,
                AuditActions.PROPERTY_LEGAL_DOCUMENT_DELETED,
                AuditActions.PROPERTY_LEGAL_DOCUMENT,
                documentId,
                oldValue,
                null
        );
    }

    private Property requireActiveProperty(Long propertyId) {
        Property property = propertyRepository.findWithUpdateDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property not found");
        }
        return property;
    }

    private PropertyLegalDocument requireDocument(Long propertyId, Long documentId) {
        return propertyLegalDocumentRepository.findByIdAndPropertyId(documentId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property legal document not found"));
    }

    private void requireCanAccess(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
        boolean assignedToActor = property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!createdByActor && !assignedToActor) {
            throw new AccessDeniedException(
                    "Agents can only access legal documents for properties they created or are assigned"
            );
        }
    }

    private void requireManagerOrAdmin(AuthUserPrincipal actor) {
        if (!isManagerOrAdmin(actor)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BusinessException("Value must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    private com.javaweb.auth.entity.User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private Map<String, Object> toValueMap(PropertyLegalDocument document) {
        return Map.of(
                "documentType", document.getDocumentType() == null ? "" : document.getDocumentType().name(),
                "documentNumber", document.getDocumentNumber() == null ? "" : document.getDocumentNumber(),
                "issuedBy", document.getIssuedBy() == null ? "" : document.getIssuedBy(),
                "issuedDate", document.getIssuedDate() == null ? "" : document.getIssuedDate().toString(),
                "expiryDate", document.getExpiryDate() == null ? "" : document.getExpiryDate().toString(),
                "verificationStatus",
                document.getVerificationStatus() == null ? "" : document.getVerificationStatus().name(),
                "storageKey", document.getStorageKey() == null ? "" : document.getStorageKey(),
                "documentUrl", document.getDocumentUrl() == null ? "" : document.getDocumentUrl(),
                "fileName", document.getFileName() == null ? "" : document.getFileName(),
                "notes", document.getNotes() == null ? "" : document.getNotes()
        );
    }
}
