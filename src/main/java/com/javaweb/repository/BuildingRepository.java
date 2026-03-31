package com.javaweb.repository;

import com.javaweb.repository.entity.BuildingEntity;

import java.util.List;
import java.util.Map;

public interface BuildingRepository {
    List<BuildingEntity> findAll(Map<String, String> param, List<String>typeCode);
}
