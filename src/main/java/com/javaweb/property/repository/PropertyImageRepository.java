package com.javaweb.property.repository;

import com.javaweb.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    List<PropertyImage> findAllByPropertyIdOrderByCoverImageDescDisplayOrderAsc(Long propertyId);

    @EntityGraph(attributePaths = {"uploadedBy", "fileResource"})
    List<PropertyImage> findAllByPropertyIdOrderByCoverImageDescDisplayOrderAscIdAsc(Long propertyId);

    Optional<PropertyImage> findFirstByPropertyIdAndCoverImageTrue(Long propertyId);

    Optional<PropertyImage> findByStorageKey(String storageKey);

    @EntityGraph(attributePaths = {"uploadedBy", "fileResource"})
    Optional<PropertyImage> findByIdAndPropertyId(Long id, Long propertyId);

    @EntityGraph(attributePaths = {
            "uploadedBy",
            "fileResource",
            "property",
            "property.createdBy",
            "property.assignedAgent"
    })
    Optional<PropertyImage> findWithAnalysisDetailsById(Long id);
}
