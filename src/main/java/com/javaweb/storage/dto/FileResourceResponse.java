package com.javaweb.storage.dto;

import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;

import java.time.Instant;

public record FileResourceResponse(
        Long id,
        String originalFileName,
        String contentType,
        long fileSize,
        String checksumSha256,
        StorageProvider storageProvider,
        FileAccessLevel accessLevel,
        String publicUrl,
        Long uploadedById,
        Instant createdAt
) {
    public static FileResourceResponse from(FileResource resource) {
        return new FileResourceResponse(
                resource.getId(),
                resource.getOriginalFileName(),
                resource.getContentType(),
                resource.getFileSize(),
                resource.getChecksumSha256(),
                resource.getStorageProvider(),
                resource.getAccessLevel(),
                resource.getPublicUrl(),
                resource.getUploadedBy().getId(),
                resource.getCreatedAt()
        );
    }
}
