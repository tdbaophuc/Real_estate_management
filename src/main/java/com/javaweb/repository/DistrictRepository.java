package com.javaweb.repository;

import com.javaweb.repository.entity.DistrictEntity;

import java.util.List;

public interface DistrictRepository {
    DistrictEntity findNameById(Integer id);
}
