package com.javaweb.property.repository;

import com.javaweb.property.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByProvinceIdAndDistrictIdAndWardId(
            Long provinceId,
            Long districtId,
            Long wardId
    );
}
