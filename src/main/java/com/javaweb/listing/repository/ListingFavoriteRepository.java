package com.javaweb.listing.repository;

import com.javaweb.listing.entity.ListingFavorite;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingFavoriteRepository extends JpaRepository<ListingFavorite, Long> {
    boolean existsByListingIdAndUserId(Long listingId, Long userId);

    long deleteByListingIdAndUserId(Long listingId, Long userId);

    Page<ListingFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "listing",
            "listing.property",
            "listing.property.propertyType",
            "listing.property.address",
            "listing.property.address.province",
            "listing.property.address.district",
            "listing.property.address.ward"
    })
    Page<ListingFavorite>
    findAllByUserIdAndListingStatusAndListingVisibilityAndListingDeletedAtIsNullAndListingPropertyDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            ListingStatus status,
            ListingVisibility visibility,
            Pageable pageable
    );
}
