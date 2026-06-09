package com.javaweb.storage.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.storage.dto.FileResourceResponse;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import com.javaweb.storage.repository.FileResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileResourceService {
    private final FileResourceRepository fileResourceRepository;
    private final UserRepository userRepository;
    private final FileUploadValidator validator;
    private final FileStorageService fileStorageService;

    public FileResourceService(
            FileResourceRepository fileResourceRepository,
            UserRepository userRepository,
            FileUploadValidator validator,
            FileStorageService fileStorageService
    ) {
        this.fileResourceRepository = fileResourceRepository;
        this.userRepository = userRepository;
        this.validator = validator;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public FileResourceResponse upload(
            MultipartFile file,
            FileAccessLevel accessLevel,
            AuthUserPrincipal actor
    ) {
        validator.validate(file);
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
                    StorageProvider.LOCAL,
                    resolvedAccess,
                    stored.publicUrl()
            );
            return FileResourceResponse.from(fileResourceRepository.saveAndFlush(resource));
        } catch (RuntimeException exception) {
            fileStorageService.delete(stored.storageKey());
            throw exception;
        }
    }
}
