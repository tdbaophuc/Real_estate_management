package com.javaweb.converter;

import com.javaweb.builder.BuildingSearchBuilder;
import com.javaweb.utils.MapUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BuildingSearchBuilderConverter {
    public BuildingSearchBuilder toBuildingSearchBuilder(Map<String, Object> param, List<String> typeCode){
        BuildingSearchBuilder buildingSearchBuilder = new BuildingSearchBuilder.Builder()
                .setName(MapUtil.getObject(param, "name", String.class))
                .setWard(MapUtil.getObject(param, "ward", String.class))
                .setStreet(MapUtil.getObject(param, "street", String.class))
                .setNumberOfBasement(MapUtil.getObject(param, "numberOfBasement", Integer.class))
                .setDirection(MapUtil.getObject(param, "direction", String.class))
                .setFloorArea(MapUtil.getObject(param, "floorArea", Integer.class))
                .setLevel(MapUtil.getObject(param, "level", String.class))
                .setManageName(MapUtil.getObject(param, "manageName", String.class))
                .setManagePhone(MapUtil.getObject(param, "managePhone", String.class))
                .setPriceFrom(MapUtil.getObject(param, "priceFrom", Integer.class))
                .setPriceTo(MapUtil.getObject(param, "priceTo", Integer.class))
                .setDistrictId(MapUtil.getObject(param, "district_id", Integer.class))
                .setStaffId(MapUtil.getObject(param, "staff_id", Integer.class))
                .setRentAreaFrom(MapUtil.getObject(param, "rentAreaFrom", Long.class))
                .setRentAreaTo(MapUtil.getObject(param, "rentAreaTo", Long.class))
                .setTypeCode(typeCode)
                .build();
        return buildingSearchBuilder;
    }
}
