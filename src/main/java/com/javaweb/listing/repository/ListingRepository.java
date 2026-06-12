package com.javaweb.listing.repository;

import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    Optional<Listing> findByCode(String code);

    Optional<Listing> findBySlugAndDeletedAtIsNull(String slug);

    @EntityGraph(attributePaths = {
            "property",
            "property.propertyType",
            "property.address",
            "property.address.province",
            "property.address.district",
            "property.address.ward"
    })
    Optional<Listing> findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
            String slug,
            ListingStatus status,
            ListingVisibility visibility
    );

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

    @EntityGraph(attributePaths = {
            "property",
            "property.propertyType",
            "property.address",
            "property.address.province",
            "property.address.district",
            "property.address.ward",
            "property.createdBy",
            "property.assignedAgent",
            "property.amenities",
            "property.amenities.amenity",
            "createdBy"
    })
    Optional<Listing> findWithAiDescriptionDetailsById(Long id);

    List<Listing> findAllByPropertyIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long propertyId);

    Page<Listing> findAllByCreatedByIdAndDeletedAtIsNull(Long createdById, Pageable pageable);

    @EntityGraph(attributePaths = {
            "property",
            "property.propertyType",
            "property.address",
            "property.address.province",
            "property.address.district",
            "property.address.ward"
    })
    @Query("""
            select listing
            from Listing listing
            where listing.status = com.javaweb.listing.enums.ListingStatus.PUBLISHED
              and listing.visibility = com.javaweb.listing.enums.ListingVisibility.PUBLIC
              and listing.deletedAt is null
              and listing.property.deletedAt is null
              and (:purpose is null or listing.purpose = :purpose)
              and (:propertyTypeId is null or listing.property.propertyType.id = :propertyTypeId)
              and (:provinceId is null or listing.property.address.province.id = :provinceId)
              and (:districtId is null or listing.property.address.district.id = :districtId)
              and (:wardId is null or listing.property.address.ward.id = :wardId)
              and (:minBudget is null or listing.askingPrice is null or listing.askingPrice >= :minBudget)
              and (:maxBudget is null or listing.askingPrice is null or listing.askingPrice <= :maxBudget)
              and (
                    :minArea is null
                    or coalesce(listing.property.floorArea, listing.property.landArea) is null
                    or coalesce(listing.property.floorArea, listing.property.landArea) >= :minArea
              )
              and (
                    :maxArea is null
                    or coalesce(listing.property.floorArea, listing.property.landArea) is null
                    or coalesce(listing.property.floorArea, listing.property.landArea) <= :maxArea
              )
              and (:minBedrooms is null or listing.property.bedrooms is null or listing.property.bedrooms >= :minBedrooms)
              and (:minBathrooms is null or listing.property.bathrooms is null or listing.property.bathrooms >= :minBathrooms)
            """)
    Page<Listing> findRecommendationCandidates(
            @Param("purpose") ListingPurpose purpose,
            @Param("propertyTypeId") Long propertyTypeId,
            @Param("provinceId") Long provinceId,
            @Param("districtId") Long districtId,
            @Param("wardId") Long wardId,
            @Param("minBudget") BigDecimal minBudget,
            @Param("maxBudget") BigDecimal maxBudget,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("minBathrooms") Integer minBathrooms,
            Pageable pageable
    );

    Page<Listing> findAllByStatusAndVisibilityAndDeletedAtIsNull(
            ListingStatus status,
            ListingVisibility visibility,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Listing listing
            set listing.viewCount = listing.viewCount + 1
            where listing.id = :listingId
            """)
    int incrementViewCount(@Param("listingId") Long listingId);
}
