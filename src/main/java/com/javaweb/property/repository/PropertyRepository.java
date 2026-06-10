package com.javaweb.property.repository;

import com.javaweb.property.entity.Property;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository
        extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    Optional<Property> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @EntityGraph(attributePaths = {
            "propertyType",
            "address",
            "address.province",
            "address.district",
            "address.ward",
            "owner",
            "createdBy",
            "assignedAgent"
    })
    Optional<Property> findWithCoreDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "propertyType",
            "address",
            "address.province",
            "address.district",
            "address.ward",
            "owner",
            "owner.roles",
            "createdBy",
            "assignedAgent",
            "assignedAgent.roles",
            "amenities",
            "amenities.amenity"
    })
    Optional<Property> findWithUpdateDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "propertyType",
            "address",
            "address.province",
            "address.district",
            "address.ward",
            "owner",
            "createdBy",
            "assignedAgent",
            "amenities",
            "amenities.amenity"
    })
    @Query("""
            select distinct property
            from Property property
            where property.id = :id
              and property.deletedAt is null
            """)
    Optional<Property> findActiveDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "propertyType",
            "address",
            "address.province",
            "address.district",
            "address.ward",
            "owner",
            "createdBy",
            "assignedAgent",
            "amenities",
            "amenities.amenity"
    })
    @Query("""
            select distinct property
            from Property property
            where property.id in :ids
            """)
    List<Property> findAllWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    Page<Property> findAllByPropertyTypeIdAndStatusAndDeletedAtIsNull(
            Long propertyTypeId,
            PropertyStatus status,
            Pageable pageable
    );

    Page<Property> findAllByPurposeAndStatusAndDeletedAtIsNull(
            PropertyPurpose purpose,
            PropertyStatus status,
            Pageable pageable
    );

    Page<Property> findAllByCreatedByIdAndDeletedAtIsNull(Long createdById, Pageable pageable);
}
