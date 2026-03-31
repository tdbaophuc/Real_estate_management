package com.javaweb.service.impl;

import com.javaweb.converter.BuildingDTOConverter;
import com.javaweb.model.BuildingDTO;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.DistrictRepository;
import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.repository.entity.DistrictEntity;
import com.javaweb.repository.entity.RentAreaEntity;
import com.javaweb.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BuildingServiceImpl implements BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private BuildingDTOConverter buildingDTOConverter;

    @Override
    public List<BuildingDTO> findAll(Map<String, String> param,List<String> buildingType) {
        List<BuildingDTO> result = new ArrayList<BuildingDTO>();
        List<BuildingEntity> buildingEntities = buildingRepository.findAll(param, buildingType);

        for (BuildingEntity  item:buildingEntities)
        {
           BuildingDTO building = buildingDTOConverter.toBuildingDTO(item);
           result.add(building);
        }
        return result;
    }
}
