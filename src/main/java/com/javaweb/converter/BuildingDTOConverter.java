package com.javaweb.converter;

import com.javaweb.builder.BuildingSearchBuilder;
import com.javaweb.model.BuildingDTO;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.DistrictRepository;
import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.repository.entity.DistrictEntity;
import com.javaweb.repository.entity.RentAreaEntity;
import org.modelmapper.ModelMapper;
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

    @Autowired
    private ModelMapper modelMapper;

    public BuildingDTO toBuildingDTO(BuildingEntity item, BuildingSearchBuilder builder){
        BuildingDTO building = modelMapper.map(item,BuildingDTO.class);
        DistrictEntity districtEntity = districtRepository.findNameById(item.getDistrict().getId());
        building.setAddress(item.getStreet()+","+item.getWard()+","+districtEntity.getName());
        List<RentAreaEntity> rentArea = rentAreaRepository.getRentAreaByBuildingID(item.getId());
        
        // Cách 2: Lọc lại rentArea trong Java dựa trên builder
        Long rentAreaFrom = builder.getRentAreaFrom();
        Long rentAreaTo = builder.getRentAreaTo();
        
        String rentAreaResult = rentArea.stream()
                .filter(it -> (rentAreaFrom == null || it.getValue() >= rentAreaFrom))
                .filter(it -> (rentAreaTo == null || it.getValue() <= rentAreaTo))
                .map(it -> it.getValue().toString())
                .collect(Collectors.joining(","));
        
        building.setRentArea(rentAreaResult);
        return building;
    };
}
