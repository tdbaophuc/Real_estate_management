package com.javaweb.repository.impl;

import com.javaweb.builder.BuildingSearchBuilder;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.entity.BuildingEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BuildingRepositoryImpl implements BuildingRepository {
    public List<BuildingEntity> findAll(BuildingSearchBuilder buildingSearchBuilder){
        return List.of();
    }
}
