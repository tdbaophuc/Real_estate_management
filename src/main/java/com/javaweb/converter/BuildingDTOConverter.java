package com.javaweb.converter;

import com.javaweb.model.BuildingDTO;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.DistrictRepository;
import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.repository.entity.DistrictEntity;
import com.javaweb.repository.entity.RentAreaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BuildingDTOConverter {
    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private RentAreaRepository rentAreaRepository;

    public BuildingDTO toBuildingDTO(BuildingEntity item){
        BuildingDTO building = new BuildingDTO();
        building.setName(item.getName());
        DistrictEntity districtEntity = districtRepository.findNameById(item.getDistrictId());
        building.setAddress(item.getStreet()+","+item.getWard()+","+districtEntity.getName());
        building.setNumberOfBasement(item.getNumberOfBasement());
        building.setManageName(item.getManageName());
        building.setManagePhone(item.getManagePhone());
        building.setFloorArea(item.getFloorArea());
        List<RentAreaEntity> rentArea = rentAreaRepository.getRentAreaByBuildingID(item.getId());
        String rentAreaResult = rentArea.stream().map(it -> it.getValue().toString()).collect(Collectors.joining(","));
        building.setRentArea(rentAreaResult);
        building.setPrice(item.getPrice());
        building.setServiceFee(item.getServiceFee());
        building.setBrokerageFee(item.getBrokerageFee());
        return building;
    };
}
