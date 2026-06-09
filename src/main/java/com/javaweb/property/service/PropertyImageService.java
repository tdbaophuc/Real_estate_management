package com.javaweb.property.service;

import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.property.dto.PropertyImageResponse;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyImage;
import com.javaweb.property.repository.PropertyImageRepository;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.service.FileResourceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PropertyImageService {
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileResourceService fileResourceService;

    public PropertyImageService(
            PropertyRepository propertyRepository,
            PropertyImageRepository propertyImageRepository,
            FileResourceService fileResourceService
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.fileResourceService = fileResourceService;
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> list(Long propertyId, AuthUserPrincipal actor) {
        Property property = requireActiveProperty(propertyId);
        requireCanRead(property, actor);
        return propertyImageRepository
                .findAllByPropertyIdOrderByCoverImageDescDisplayOrderAscIdAsc(propertyId)
                .stream()
                .map(PropertyImageResponse::from)
                .toList();
    }

    @Transactional
    public PropertyImageResponse upload(
            Long propertyId,
            MultipartFile file,
            String altText,
            int displayOrder,
            AuthUserPrincipal actor
    ) {
        Property property = requireActiveProperty(propertyId);
        requireCanModify(property, actor);
        FileResource resource = fileResourceService.storeImage(
                file,
                FileAccessLevel.PUBLIC,
                actor
        );

        try {
            PropertyImage image = new PropertyImage(resource.getUploadedBy(), resource);
            image.setAltText(normalizeAltText(altText));
            image.setDisplayOrder(displayOrder);
            image.setCoverImage(
                    propertyImageRepository.findFirstByPropertyIdAndCoverImageTrue(propertyId)
                            .isEmpty()
            );
            property.addImage(image);
            return PropertyImageResponse.from(propertyImageRepository.saveAndFlush(image));
        } catch (RuntimeException exception) {
            fileResourceService.delete(resource);
            throw exception;
        }
    }

    @Transactional
    public void delete(Long propertyId, Long imageId, AuthUserPrincipal actor) {
        Property property = requireActiveProperty(propertyId);
        requireCanModify(property, actor);
        PropertyImage image = requireImage(propertyId, imageId);
        boolean wasCover = image.isCoverImage();
        FileResource resource = image.getFileResource();

        property.removeImage(image);
        propertyImageRepository.delete(image);
        propertyImageRepository.flush();
        if (wasCover) {
            propertyImageRepository
                    .findAllByPropertyIdOrderByCoverImageDescDisplayOrderAscIdAsc(propertyId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> next.setCoverImage(true));
        }
        if (resource != null) {
            fileResourceService.delete(resource);
        }
    }

    @Transactional
    public PropertyImageResponse setCover(
            Long propertyId,
            Long imageId,
            AuthUserPrincipal actor
    ) {
        Property property = requireActiveProperty(propertyId);
        requireCanModify(property, actor);
        PropertyImage selected = requireImage(propertyId, imageId);
        propertyImageRepository
                .findAllByPropertyIdOrderByCoverImageDescDisplayOrderAscIdAsc(propertyId)
                .forEach(image -> image.setCoverImage(image.getId().equals(selected.getId())));
        return PropertyImageResponse.from(selected);
    }

    private Property requireActiveProperty(Long propertyId) {
        Property property = propertyRepository.findWithUpdateDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property not found");
        }
        return property;
    }

    private PropertyImage requireImage(Long propertyId, Long imageId) {
        return propertyImageRepository.findByIdAndPropertyId(imageId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property image not found"));
    }

    private void requireCanRead(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
        boolean assignedToActor = property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!createdByActor && !assignedToActor) {
            throw new AccessDeniedException(
                    "Agents can only view images for properties they created or are assigned"
            );
        }
    }

    private void requireCanModify(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (!property.getCreatedBy().getId().equals(actor.id())) {
            throw new AccessDeniedException(
                    "Agents can only modify images for properties they created"
            );
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private String normalizeAltText(String altText) {
        if (altText == null || altText.isBlank()) {
            return null;
        }
        String normalized = altText.trim();
        if (normalized.length() > 255) {
            throw new BusinessException("altText must not exceed 255 characters");
        }
        return normalized;
    }
}
