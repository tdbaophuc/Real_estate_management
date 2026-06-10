package com.javaweb.property.repository;

import com.javaweb.property.entity.Amenity;
import com.javaweb.property.enums.AmenityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByCode(String code);

    List<Amenity> findAllByIdInAndActiveTrue(Set<Long> ids);

    List<Amenity> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Amenity> findAllByCategoryAndActiveTrueOrderByDisplayOrderAsc(AmenityCategory category);
}
