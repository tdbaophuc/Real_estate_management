package com.javaweb.property.dto;

import com.javaweb.property.entity.PropertyImage;

import java.time.Instant;

public record PropertyImageResponse(
        Long id,
        String imageUrl,
        String fileName,
        String mimeType,
        Long fileSize,
        String altText,
        boolean coverImage,
        int displayOrder,
        Long uploadedById,
        String uploadedByName,
        Instant createdAt
) {
    public static PropertyImageResponse from(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getFileName(),
                image.getMimeType(),
                image.getFileSize(),
                image.getAltText(),
                image.isCoverImage(),
                image.getDisplayOrder(),
                image.getUploadedBy().getId(),
                image.getUploadedBy().getFullName(),
                image.getCreatedAt()
        );
    }
}
