package com.javaweb.storage.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.repository.ContractDocumentRepository;
import com.javaweb.property.enums.DocumentVerificationStatus;
import com.javaweb.property.repository.PropertyImageRepository;
import com.javaweb.property.repository.PropertyLegalDocumentRepository;
import com.javaweb.storage.dto.FileDownloadResult;
import com.javaweb.storage.dto.FileResourceResponse;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.repository.FileResourceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class FileResourceService {
    private final FileResourceRepository fileResourceRepository;
    private final UserRepository userRepository;
    private final FileUploadValidator validator;
    private final FileStorageService fileStorageService;
    private final ContractDocumentRepository contractDocumentRepository;
    private final PropertyLegalDocumentRepository propertyLegalDocumentRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final AuditLogService auditLogService;

    public FileResourceService(
            FileResourceRepository fileResourceRepository,
            UserRepository userRepository,
            FileUploadValidator validator,
            FileStorageService fileStorageService,
            ContractDocumentRepository contractDocumentRepository,
            PropertyLegalDocumentRepository propertyLegalDocumentRepository,
            PropertyImageRepository propertyImageRepository,
            AuditLogService auditLogService
    ) {
        this.fileResourceRepository = fileResourceRepository;
        this.userRepository = userRepository;
        this.validator = validator;
        this.fileStorageService = fileStorageService;
        this.contractDocumentRepository = contractDocumentRepository;
        this.propertyLegalDocumentRepository = propertyLegalDocumentRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FileResourceResponse upload(
            MultipartFile file,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        return FileResourceResponse.from(store(file, accessLevel, actor));
    }

    @Transactional
    public FileResource store(
            MultipartFile file,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        validator.validate(file);
        return storeValidated(file, accessLevel, actor);
    }

    @Transactional
    public FileResource storeImage(
            MultipartFile file,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        validator.validateImage(file);
        return storeValidated(file, accessLevel, actor);
    }

    private FileResource storeValidated(
            MultipartFile file,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        User uploader = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        FileAccessLevel resolvedAccess = accessLevel == null
                ? FileAccessLevel.PRIVATE
                : accessLevel;
        StoredFile stored = fileStorageService.store(file, resolvedAccess);
        try {
            FileResource resource = new FileResource(
                    uploader,
                    StringUtils.cleanPath(file.getOriginalFilename()),
                    stored.storageKey(),
                    file.getContentType(),
                    file.getSize(),
                    stored.checksumSha256(),
                    fileStorageService.provider(),
                    resolvedAccess,
                    stored.publicUrl()
            );
            return fileResourceRepository.saveAndFlush(resource);
        } catch (RuntimeException exception) {
            fileStorageService.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional
    public void delete(FileResource resource) {
        fileResourceRepository.delete(resource);
        fileResourceRepository.flush();
        fileStorageService.delete(resource.getStorageKey());
    }

    @Transactional(readOnly = true)
    public FileResourceResponse get(Long fileId, AuthUserPrincipal actor) {
        FileResource resource = findAuthorized(fileId, actor);
        return FileResourceResponse.from(resource);
    }

    @Transactional
    public FileDownloadResult download(Long fileId, AuthUserPrincipal actor) {
        FileResource resource = findAuthorized(fileId, actor);
        auditLogService.record(
                actor,
                AuditActions.FILE_DOWNLOADED,
                AuditActions.FILE,
                resource.getId(),
                null,
                Map.of(
                        "storageProvider", resource.getStorageProvider().name(),
                        "accessLevel", resource.getAccessLevel().name()
                )
        );
        if (resource.getAccessLevel() == FileAccessLevel.PUBLIC
                && StringUtils.hasText(resource.getPublicUrl())
                && isSafeDownloadUrl(resource.getPublicUrl())) {
            return new FileDownloadResult(resource, null, resource.getPublicUrl());
        }
        String signedUrl = fileStorageService.createDownloadUrl(resource.getStorageKey());
        if (StringUtils.hasText(signedUrl)) {
            return new FileDownloadResult(resource, null, signedUrl);
        }
        return new FileDownloadResult(
                resource,
                fileStorageService.load(resource.getStorageKey()),
                null
        );
    }

    @Transactional
    public void delete(Long fileId, AuthUserPrincipal actor) {
        FileResource resource = findAuthorized(fileId, actor);
        assertDeletable(resource);
        fileResourceRepository.delete(resource);
        fileResourceRepository.flush();
        fileStorageService.delete(resource.getStorageKey());
        auditLogService.record(
                actor,
                AuditActions.FILE_DELETED,
                AuditActions.FILE,
                fileId,
                Map.of(
                        "storageKey", resource.getStorageKey(),
                        "storageProvider", resource.getStorageProvider().name(),
                        "accessLevel", resource.getAccessLevel().name()
                ),
                null
        );
    }

    @Transactional
    public FileResourceResponse updateAccessLevel(
            Long fileId,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        if (!hasRole(actor, "ADMIN") && !hasRole(actor, "MANAGER")) {
            throw new AccessDeniedException("Access is denied");
        }
        FileResource resource = fileResourceRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File resource not found"));
        FileAccessLevel target = accessLevel == null ? FileAccessLevel.PRIVATE : accessLevel;
        FileAccessLevel oldAccess = resource.getAccessLevel();
        String oldStorageKey = resource.getStorageKey();
        String oldPublicUrl = resource.getPublicUrl();
        if (oldAccess != target) {
            StoredFile updated = fileStorageService.changeAccessLevel(resource.getStorageKey(), target);
            resource.updateStorage(
                    updated.storageKey(),
                    updated.checksumSha256(),
                    target,
                    updated.publicUrl()
            );
        }
        FileResource saved = fileResourceRepository.saveAndFlush(resource);
        auditLogService.record(
                actor,
                AuditActions.FILE_ACCESS_LEVEL_CHANGED,
                AuditActions.FILE,
                saved.getId(),
                Map.of(
                        "accessLevel", oldAccess.name(),
                        "storageKey", oldStorageKey,
                        "publicUrl", oldPublicUrl == null ? "" : oldPublicUrl
                ),
                Map.of(
                        "accessLevel", saved.getAccessLevel().name(),
                        "storageKey", saved.getStorageKey(),
                        "publicUrl", saved.getPublicUrl() == null ? "" : saved.getPublicUrl()
                )
        );
        return FileResourceResponse.from(saved);
    }

    private FileResource findAuthorized(Long fileId, AuthUserPrincipal actor) {
        FileResource resource = fileResourceRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File resource not found"));
        if (!canAccess(resource, actor)) {
            throw new AccessDeniedException("Access is denied");
        }
        return resource;
    }

    private boolean canAccess(FileResource resource, AuthUserPrincipal actor) {
        return hasRole(actor, "ADMIN")
                || hasRole(actor, "MANAGER")
                || resource.getUploadedBy().getId().equals(actor.id());
    }

    private boolean hasRole(AuthUserPrincipal actor, String role) {
        return actor.roles().contains(role);
    }

    private void assertDeletable(FileResource resource) {
        if (contractDocumentRepository.existsByFileResourceIdAndContractStatus(
                resource.getId(),
                ContractStatus.SIGNED
        )) {
            throw new BusinessException("Signed contract files cannot be deleted");
        }
        if (propertyLegalDocumentRepository.existsByStorageKeyAndVerificationStatus(
                resource.getStorageKey(),
                DocumentVerificationStatus.VERIFIED
        )) {
            throw new BusinessException("Verified legal document files cannot be deleted");
        }
        if (contractDocumentRepository.existsByFileResourceId(resource.getId())
                || propertyImageRepository.existsByFileResourceId(resource.getId())
                || propertyLegalDocumentRepository.existsByStorageKey(resource.getStorageKey())) {
            throw new BusinessException("Linked files cannot be deleted");
        }
    }

    private boolean isSafeDownloadUrl(String url) {
        return url.startsWith("/uploads/")
                || url.startsWith("https://")
                || url.startsWith("http://localhost:")
                || url.startsWith("http://127.0.0.1:");
    }
}
