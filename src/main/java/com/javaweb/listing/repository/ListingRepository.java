package com.javaweb.listing.repository;

import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    Optional<Listing> findByCode(String code);

    Optional<Listing> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsByCode(String code);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {
            "property",
            "property.propertyType",
            "property.address",
            "createdBy",
            "reviewedBy",
            "listingPackage"
    })
    Optional<Listing> findWithCoreDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "property",
            "createdBy",
            "reviewedBy",
            "listingPackage"
    })
    Optional<Listing> findWithUpdateDetailsById(Long id);

    List<Listing> findAllByPropertyIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long propertyId);

    Page<Listing> findAllByCreatedByIdAndDeletedAtIsNull(Long createdById, Pageable pageable);

    Page<Listing> findAllByStatusAndVisibilityAndDeletedAtIsNull(
            ListingStatus status,
            ListingVisibility visibility,
            Pageable pageable
    );
}
