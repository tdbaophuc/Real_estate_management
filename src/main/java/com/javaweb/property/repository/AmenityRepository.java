package com.javaweb.property.repository;

import com.javaweb.property.entity.Amenity;
import com.javaweb.property.enums.AmenityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByCode(String code);

    List<Amenity> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Amenity> findAllByCategoryAndActiveTrueOrderByDisplayOrderAsc(AmenityCategory category);
}
