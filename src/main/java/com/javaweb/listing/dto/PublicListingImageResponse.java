package com.javaweb.listing.dto;

public record PublicListingImageResponse(
        Long id,
        String imageUrl,
        String altText,
        boolean coverImage,
        int displayOrder
) {
}
