package com.javaweb.service;

import com.javaweb.model.BuildingDTO;

import java.util.List;
import java.util.Map;

public interface BuildingService  {
    List<BuildingDTO> findAll(Map<String, String> param, List<String> buildingType);
}
