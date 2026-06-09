package com.javaweb.property.repository;

import com.javaweb.property.entity.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyTypeRepository extends JpaRepository<PropertyType, Long> {
    Optional<PropertyType> findByCode(String code);

    Optional<PropertyType> findByIdAndActiveTrue(Long id);

    List<PropertyType> findAllByActiveTrueOrderByDisplayOrderAsc();
}
