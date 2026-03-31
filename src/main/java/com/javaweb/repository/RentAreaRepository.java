package com.javaweb.repository;

import com.javaweb.repository.entity.RentAreaEntity;

import java.util.List;

public interface RentAreaRepository {
    List<RentAreaEntity> getRentAreaByBuildingID(Integer building_id);
}
