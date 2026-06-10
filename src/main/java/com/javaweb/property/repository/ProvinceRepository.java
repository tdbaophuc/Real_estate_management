package com.javaweb.property.repository;

import com.javaweb.property.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long> {
    Optional<Province> findByCode(String code);

    Optional<Province> findByIdAndActiveTrue(Long id);

    List<Province> findAllByActiveTrueOrderByNameAsc();
}
