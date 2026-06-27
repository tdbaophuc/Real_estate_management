package com.javaweb.listing.mapper;

import com.javaweb.auth.entity.User;
import com.javaweb.listing.dto.InternalListingDetailResponse;
import com.javaweb.listing.dto.InternalListingPackageSummary;
import com.javaweb.listing.dto.InternalListingPropertySummary;
import com.javaweb.listing.dto.InternalListingStatusHistoryResponse;
import com.javaweb.listing.dto.InternalListingUserSummary;
import com.javaweb.listing.dto.ListingCreateRequest;
import com.javaweb.listing.dto.ListingResponse;
import com.javaweb.listing.dto.ListingUpdateRequest;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.entity.ListingStatusHistory;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.Ward;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
public class ListingMapper {

    public Listing toEntity(
            ListingCreateRequest request,
            Property property,
            User creator,
            ListingPackage listingPackage
    ) {
        Listing listing = new Listing(
                request.code(),
                property,
                creator,
                request.title(),
                request.slug(),
                request.description(),
                request.purpose()
        );
        listing.setStatus(ListingStatus.DRAFT);
        listing.setListingPackage(listingPackage);
        applyPublicFields(
                listing,
                request.visibility(),
                request.askingPrice(),
                request.currency(),
                request.seoTitle(),
                request.seoDescription(),
                request.seoKeywords()
        );
        return listing;
    }

    public void updateEntity(
            Listing listing,
            ListingUpdateRequest request,
            ListingPackage listingPackage
    ) {
        listing.setTitle(request.title());
        listing.setSlug(request.slug());
        listing.setDescription(request.description());
        listing.setPurpose(request.purpose());
        listing.setListingPackage(listingPackage);
        applyPublicFields(
                listing,
                request.visibility(),
                request.askingPrice(),
                request.currency(),
                request.seoTitle(),
                request.seoDescription(),
                request.seoKeywords()
        );
    }

    public ListingResponse toResponse(Listing listing) {
        Property property = listing.getProperty();
        User creator = listing.getCreatedBy();
        User reviewer = listing.getReviewedBy();
        ListingPackage listingPackage = listing.getListingPackage();

        return new ListingResponse(
                listing.getId(),
                listing.getCode(),
                property.getId(),
                property.getCode(),
                property.getName(),
                creator.getId(),
                creator.getFullName(),
                listingPackage == null ? null : listingPackage.getId(),
                listingPackage == null ? null : listingPackage.getCode(),
                listingPackage == null ? null : listingPackage.getName(),
                listing.getTitle(),
                listing.getSlug(),
                listing.getDescription(),
                listing.getPurpose(),
                listing.getStatus(),
                listing.getVisibility(),
                listing.getAskingPrice(),
                listing.getCurrency(),
                listing.getSeoTitle(),
                listing.getSeoDescription(),
                listing.getSeoKeywords(),
                reviewer == null ? null : reviewer.getId(),
                reviewer == null ? null : reviewer.getFullName(),
                listing.getRejectionReason(),
                listing.getSubmittedAt(),
                listing.getReviewedAt(),
                listing.getPublishedAt(),
                listing.getUnpublishedAt(),
                listing.getViewCount(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    public PublicListingResponse toPublicResponse(Listing listing) {
        Property property = listing.getProperty();
        Address address = property.getAddress();
        District district = address.getDistrict();
        Ward ward = address.getWard();

        return new PublicListingResponse(
                listing.getId(),
                listing.getCode(),
                property.getId(),
                property.getCode(),
                property.getName(),
                property.getPropertyType().getId(),
                property.getPropertyType().getName(),
                listing.getTitle(),
                listing.getSlug(),
                listing.getDescription(),
                listing.getPurpose(),
                listing.getStatus(),
                listing.getAskingPrice(),
                listing.getCurrency(),
                property.getLandArea(),
                property.getFloorArea(),
                property.getBedrooms(),
                property.getBathrooms(),
                address.getProvince().getId(),
                address.getProvince().getName(),
                district == null ? null : district.getId(),
                district == null ? null : district.getName(),
                ward == null ? null : ward.getId(),
                ward == null ? null : ward.getName(),
                address.getStreetAddress(),
                address.getFullAddress(),
                listing.getViewCount(),
                listing.getPublishedAt(),
                listing.getCreatedAt()
        );
    }

    public InternalListingDetailResponse toInternalDetailResponse(
            Listing listing,
            long favoriteCount
    ) {
        return new InternalListingDetailResponse(
                listing.getId(),
                listing.getCode(),
                listing.getTitle(),
                listing.getSlug(),
                listing.getDescription(),
                listing.getPurpose(),
                listing.getStatus(),
                listing.getVisibility(),
                listing.getAskingPrice(),
                listing.getCurrency(),
                listing.getSeoTitle(),
                listing.getSeoDescription(),
                listing.getSeoKeywords(),
                listing.getRejectionReason(),
                listing.getSubmittedAt(),
                listing.getReviewedAt(),
                listing.getPublishedAt(),
                listing.getUnpublishedAt(),
                listing.getExpiresAt(),
                listing.getFeaturedUntil(),
                listing.getViewCount(),
                favoriteCount,
                toPropertySummary(listing.getProperty()),
                toUserSummary(listing.getCreatedBy()),
                toUserSummary(listing.getReviewedBy()),
                toPackageSummary(listing.getListingPackage()),
                listing.getStatusHistories().stream()
                        .sorted(Comparator.comparing(
                                ListingStatusHistory::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .map(this::toStatusHistoryResponse)
                        .toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    private InternalListingPropertySummary toPropertySummary(Property property) {
        Address address = property.getAddress();
        return new InternalListingPropertySummary(
                property.getId(),
                property.getCode(),
                property.getName(),
                property.getPropertyType().getId(),
                property.getPropertyType().getCode(),
                property.getPropertyType().getName(),
                property.getPurpose(),
                property.getStatus(),
                property.getPrice(),
                property.getCurrency(),
                property.getLandArea(),
                property.getFloorArea(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getCreatedBy().getId(),
                property.getCreatedBy().getFullName(),
                property.getAssignedAgent() == null ? null : property.getAssignedAgent().getId(),
                property.getAssignedAgent() == null ? null : property.getAssignedAgent().getFullName(),
                address == null ? null : address.getFullAddress()
        );
    }

    private InternalListingUserSummary toUserSummary(User user) {
        return user == null
                ? null
                : new InternalListingUserSummary(user.getId(), user.getFullName(), user.getEmail());
    }

    private InternalListingPackageSummary toPackageSummary(ListingPackage listingPackage) {
        return listingPackage == null
                ? null
                : new InternalListingPackageSummary(
                        listingPackage.getId(),
                        listingPackage.getCode(),
                        listingPackage.getName(),
                        listingPackage.getDurationDays(),
                        listingPackage.getPrice(),
                        listingPackage.isActive()
                );
    }

    private InternalListingStatusHistoryResponse toStatusHistoryResponse(
            ListingStatusHistory history
    ) {
        return new InternalListingStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                toUserSummary(history.getChangedBy()),
                history.getReason(),
                history.getCreatedAt()
        );
    }

    private void applyPublicFields(
            Listing listing,
            ListingVisibility visibility,
            BigDecimal askingPrice,
            String currency,
            String seoTitle,
            String seoDescription,
            String seoKeywords
    ) {
        listing.setVisibility(visibility);
        listing.setAskingPrice(askingPrice);
        listing.setCurrency(currency);
        listing.setSeoTitle(seoTitle);
        listing.setSeoDescription(seoDescription);
        listing.setSeoKeywords(seoKeywords);
    }
}
