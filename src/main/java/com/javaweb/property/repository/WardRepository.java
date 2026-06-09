package com.javaweb.property.repository;

import com.javaweb.property.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {
    Optional<Ward> findByCode(String code);

    List<Ward> findAllByDistrictIdAndActiveTrueOrderByNameAsc(Long districtId);
}
