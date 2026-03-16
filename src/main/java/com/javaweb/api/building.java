package com.javaweb.api;

import com.javaweb.model.BuildingDTO;
import com.javaweb.customException.FiedRequireException;
import com.javaweb.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class building {
    @Autowired
    private BuildingService buildingService;

    @GetMapping(value = "/api/buildings/")
    public List<BuildingDTO> buildings( @RequestParam Map<String, String> param) {
        List<BuildingDTO> result = buildingService.findAll(param);
        return result;
    };

    @PostMapping(value= "/api/buildings/")
    public Object buildings(
            @RequestBody BuildingDTO building
    ){
        validate(building);
//        try {
//            System.out.println(1/0);
//            validate(building);
//
//        } catch (Exception e){
//            ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
//            errorResponseDTO.setError(e.getMessage());
//            List<String> detail = new ArrayList<>();
//            detail.add("xem lai age, name");
//            errorResponseDTO.setDetails(detail);
//            return errorResponseDTO;
//        }
        return building;
    }
    public void validate(BuildingDTO buildingDTO ){
        if (buildingDTO.getName() == null || buildingDTO.getName().equals("")){
            throw new FiedRequireException("age, name is null");
        }
    }


    @DeleteMapping(value="/api/buildings/{id}")
    public void deleteBuildings(@PathVariable Integer id){
        System.out.println("da xoa thanh cong id:"+id);
    }

    
}