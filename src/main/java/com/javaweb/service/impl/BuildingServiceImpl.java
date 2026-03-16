package com.javaweb.service.impl;

import com.javaweb.model.BuildingDTO;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BuildingServiceImpl implements BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;
    @Override
    public List<BuildingDTO> findAll(Map<String, String> param) {
        List<BuildingDTO> result = new ArrayList<BuildingDTO>();
        List<BuildingEntity> buildingEntities = buildingRepository.findAll(param);

        for (BuildingEntity  item:buildingEntities)
        {
            BuildingDTO building = new BuildingDTO();
            building.setName(item.getName());
            building.setAddress(item.getStreet()+","+item.getWard()+","+item.getDistrictName());
            building.setNumberOfBasement(item.getNumberOfBasement());
            building.setManageName(item.getManageName());
            building.setManagePhone(item.getManagePhone());
            building.setFloorArea(item.getFloorArea());
            building.setRentArea(item.getRentArea());
            building.setPrice(item.getPrice());
            building.setServiceFee(item.getServiceFee());
            building.setBrokerageFee(item.getBrokerageFee());

            result.add(building);
        }
        return result;
    }
}
