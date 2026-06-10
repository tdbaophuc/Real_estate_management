package com.javaweb.listing.mapper;

import com.javaweb.auth.entity.User;
import com.javaweb.listing.dto.ListingCreateRequest;
import com.javaweb.listing.dto.ListingResponse;
import com.javaweb.listing.dto.ListingUpdateRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.property.entity.Property;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
