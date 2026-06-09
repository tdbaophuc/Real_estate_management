package com.javaweb.property.repository;

import com.javaweb.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    List<PropertyImage> findAllByPropertyIdOrderByCoverImageDescDisplayOrderAsc(Long propertyId);

    Optional<PropertyImage> findFirstByPropertyIdAndCoverImageTrue(Long propertyId);

    Optional<PropertyImage> findByStorageKey(String storageKey);
}
