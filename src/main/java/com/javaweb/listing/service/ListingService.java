package com.javaweb.listing.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.listing.dto.ListingCreateRequest;
import com.javaweb.listing.dto.ListingResponse;
import com.javaweb.listing.dto.ListingUpdateRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingPackageRepository;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Property;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ListingService {
    private static final Set<ListingStatus> EDITABLE_STATUSES =
            Set.of(ListingStatus.DRAFT, ListingStatus.REJECTED);

    private final ListingRepository listingRepository;
    private final ListingPackageRepository listingPackageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;

    public ListingService(
            ListingRepository listingRepository,
            ListingPackageRepository listingPackageRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            ListingMapper listingMapper
    ) {
        this.listingRepository = listingRepository;
        this.listingPackageRepository = listingPackageRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.listingMapper = listingMapper;
    }

    @Transactional
    public ListingResponse create(ListingCreateRequest request, AuthUserPrincipal actor) {
        if (listingRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Listing code already exists");
        }
        if (listingRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Listing slug already exists");
        }

        Property property = propertyRepository.findWithUpdateDetailsById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null || property.getStatus() == PropertyStatus.DELETED) {
            throw new ResourceNotFoundException("Property not found");
        }
        requireCanCreateFromProperty(property, actor);
        requirePurposeMatchesProperty(request.purpose(), property);

        User creator = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        ListingPackage listingPackage = requireActivePackage(request.listingPackageId());
        Listing listing = listingMapper.toEntity(request, property, creator, listingPackage);

        return listingMapper.toResponse(listingRepository.saveAndFlush(listing));
    }

    @Transactional
    public ListingResponse update(
            Long listingId,
            ListingUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        Listing listing = listingRepository.findWithUpdateDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing not found");
        }
        requireCanModify(listing, actor);
        if (!EDITABLE_STATUSES.contains(listing.getStatus())) {
            throw new BusinessException("Only draft or rejected listings can be edited");
        }
        if (listingRepository.existsBySlugAndIdNot(request.slug(), listingId)) {
            throw new DuplicateResourceException("Listing slug already exists");
        }
        requirePurposeMatchesProperty(request.purpose(), listing.getProperty());

        ListingPackage listingPackage = requireActivePackage(request.listingPackageId());
        listingMapper.updateEntity(listing, request, listingPackage);

        return listingMapper.toResponse(listingRepository.saveAndFlush(listing));
    }

    private ListingPackage requireActivePackage(Long listingPackageId) {
        if (listingPackageId == null) {
            return null;
        }
        return listingPackageRepository.findByIdAndActiveTrue(listingPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Active listing package not found"));
    }

    private void requirePurposeMatchesProperty(ListingPurpose purpose, Property property) {
        if (!property.getPurpose().name().equals(purpose.name())) {
            throw new BusinessException("Listing purpose must match property purpose");
        }
    }

    private void requireCanCreateFromProperty(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
        boolean assignedToActor = property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!createdByActor && !assignedToActor) {
            throw new AccessDeniedException(
                    "Agents can only create listings for properties they created or are assigned"
            );
        }
    }

    private void requireCanModify(Listing listing, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (!listing.getCreatedBy().getId().equals(actor.id())) {
            throw new AccessDeniedException("Agents can only modify listings they created");
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }
}
