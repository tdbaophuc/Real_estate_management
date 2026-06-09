package com.javaweb.property.repository;

import com.javaweb.property.entity.PropertyAmenity;
import com.javaweb.property.entity.PropertyAmenityId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, PropertyAmenityId> {
    @EntityGraph(attributePaths = "amenity")
    List<PropertyAmenity> findAllByPropertyId(Long propertyId);

    List<PropertyAmenity> findAllByAmenityId(Long amenityId);
}
