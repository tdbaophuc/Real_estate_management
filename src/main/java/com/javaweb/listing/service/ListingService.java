package com.javaweb.listing.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
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
import com.javaweb.listing.dto.RejectListingRequest;
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

import java.time.Instant;
import java.util.Map;
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
    private final AuditLogService auditLogService;

    public ListingService(
            ListingRepository listingRepository,
            ListingPackageRepository listingPackageRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            ListingMapper listingMapper,
            AuditLogService auditLogService
    ) {
        this.listingRepository = listingRepository;
        this.listingPackageRepository = listingPackageRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.listingMapper = listingMapper;
        this.auditLogService = auditLogService;
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

    @Transactional
    public ListingResponse submit(Long listingId, AuthUserPrincipal actor) {
        Listing listing = requireListingForWorkflow(listingId);
        requireCanModify(listing, actor);
        requireCurrentStatus(
                listing,
                Set.of(ListingStatus.DRAFT, ListingStatus.REJECTED),
                "Only draft or rejected listings can be submitted"
        );

        User changedBy = requireActor(actor);
        Instant now = Instant.now();
        listing.setSubmittedAt(now);
        listing.setReviewedAt(null);
        listing.setReviewedBy(null);
        listing.setRejectionReason(null);
        transition(listing, ListingStatus.PENDING_REVIEW, changedBy, null);
        return saveWorkflowResult(listing);
    }

    @Transactional
    public ListingResponse approve(Long listingId, AuthUserPrincipal actor) {
        Listing listing = requireListingForWorkflow(listingId);
        requireCurrentStatus(
                listing,
                Set.of(ListingStatus.PENDING_REVIEW),
                "Only listings pending review can be approved"
        );

        User reviewer = requireActor(actor);
        ListingStatus previousStatus = listing.getStatus();
        listing.setReviewedBy(reviewer);
        listing.setReviewedAt(Instant.now());
        listing.setRejectionReason(null);
        transition(listing, ListingStatus.APPROVED, reviewer, null);
        ListingResponse response = saveWorkflowResult(listing);
        auditStatusChange(
                actor,
                AuditActions.LISTING_APPROVED,
                listing,
                previousStatus,
                null
        );
        return response;
    }

    @Transactional
    public ListingResponse reject(
            Long listingId,
            RejectListingRequest request,
            AuthUserPrincipal actor
    ) {
        Listing listing = requireListingForWorkflow(listingId);
        requireCurrentStatus(
                listing,
                Set.of(ListingStatus.PENDING_REVIEW),
                "Only listings pending review can be rejected"
        );

        User reviewer = requireActor(actor);
        ListingStatus previousStatus = listing.getStatus();
        listing.setReviewedBy(reviewer);
        listing.setReviewedAt(Instant.now());
        listing.setRejectionReason(request.reason());
        transition(listing, ListingStatus.REJECTED, reviewer, request.reason());
        ListingResponse response = saveWorkflowResult(listing);
        auditStatusChange(
                actor,
                AuditActions.LISTING_REJECTED,
                listing,
                previousStatus,
                request.reason()
        );
        return response;
    }

    @Transactional
    public ListingResponse publish(Long listingId, AuthUserPrincipal actor) {
        Listing listing = requireListingForWorkflow(listingId);
        requireCanModify(listing, actor);
        requireCurrentStatus(
                listing,
                Set.of(ListingStatus.APPROVED, ListingStatus.UNPUBLISHED),
                "Only approved or unpublished listings can be published"
        );

        User changedBy = requireActor(actor);
        listing.setPublishedAt(Instant.now());
        listing.setUnpublishedAt(null);
        transition(listing, ListingStatus.PUBLISHED, changedBy, null);
        return saveWorkflowResult(listing);
    }

    @Transactional
    public ListingResponse unpublish(Long listingId, AuthUserPrincipal actor) {
        Listing listing = requireListingForWorkflow(listingId);
        requireCanModify(listing, actor);
        requireCurrentStatus(
                listing,
                Set.of(ListingStatus.PUBLISHED),
                "Only published listings can be unpublished"
        );

        User changedBy = requireActor(actor);
        listing.setUnpublishedAt(Instant.now());
        transition(listing, ListingStatus.UNPUBLISHED, changedBy, null);
        return saveWorkflowResult(listing);
    }

    private ListingPackage requireActivePackage(Long listingPackageId) {
        if (listingPackageId == null) {
            return null;
        }
        return listingPackageRepository.findByIdAndActiveTrue(listingPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Active listing package not found"));
    }

    private Listing requireListingForWorkflow(Long listingId) {
        Listing listing = listingRepository.findWithUpdateDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing not found");
        }
        return listing;
    }

    private User requireActor(AuthUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void requireCurrentStatus(
            Listing listing,
            Set<ListingStatus> allowedStatuses,
            String message
    ) {
        if (!allowedStatuses.contains(listing.getStatus())) {
            throw new BusinessException(message);
        }
    }

    private void transition(
            Listing listing,
            ListingStatus targetStatus,
            User changedBy,
            String reason
    ) {
        ListingStatus previousStatus = listing.getStatus();
        listing.setStatus(targetStatus);
        listing.addStatusHistory(previousStatus, targetStatus, changedBy, reason);
    }

    private ListingResponse saveWorkflowResult(Listing listing) {
        return listingMapper.toResponse(listingRepository.saveAndFlush(listing));
    }

    private void auditStatusChange(
            AuthUserPrincipal actor,
            String action,
            Listing listing,
            ListingStatus previousStatus,
            String reason
    ) {
        Map<String, Object> newValue = new java.util.LinkedHashMap<>();
        newValue.put("status", listing.getStatus().name());
        if (reason != null) {
            newValue.put("reason", reason);
        }
        auditLogService.record(
                actor,
                action,
                AuditActions.LISTING,
                listing.getId(),
                Map.of("status", previousStatus.name()),
                newValue
        );
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
