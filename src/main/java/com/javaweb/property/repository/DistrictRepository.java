package com.javaweb.property.repository;

import com.javaweb.property.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findByCode(String code);

    Optional<District> findByIdAndActiveTrue(Long id);

    List<District> findAllByProvinceIdAndActiveTrueOrderByNameAsc(Long provinceId);
}
