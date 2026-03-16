package com.javaweb.repository.impl;

import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.entity.BuildingEntity;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class BuildingRepositoryImpl implements BuildingRepository {
    final static String URL = "jdbc:mysql://localhost:3306/java_backend_project01";
    final static String USER = "root";
    final static String PASS = "123456";
    @Override
    public List<BuildingEntity> findAll(Map<String, String> param) {
        List<BuildingEntity> results = new ArrayList<>();
        StringBuilder SQL= new StringBuilder("select distinct b.*, d.name as districtName, group_concat(r.value) as rentArea ");
        SQL.append("from building b ");
        SQL.append("inner join district d on b.district_id = d.id ");
        SQL.append("inner join assignment_building ab on b.id = ab.building_id ");
        SQL.append("inner join building_building_type bbt on bbt.building_id=b.id inner join building_type bt on bt.id=bbt.building_type_id ");
        SQL.append("left join rent_area r on r.building_id = b.id ");
        SQL.append("where 1=1 ");
        param.forEach((key,value) -> {
            if (key!=null & value!=null){
                if (key.equals("name")) { SQL.append("And b.name like '%"+value+"%' ");}
                else if (key.equals("district_id")) { SQL.append("And b.district_id = "+value+" ");}
                else if (key.equals("ward")) { SQL.append("And b.ward = "+value+" ");}
                else if (key.equals("street")) { SQL.append("And b.street like '%"+value+"%' ");}
                else if (key.equals("floorArea")) { SQL.append("And b.floorArea = "+value+" ");}
                else if (key.equals("numberOfBasement")) { SQL.append("And b.numberOfBasement = "+value+" ");}
                else if (key.equals("direction")) { SQL.append("And b.direction = "+value+" ");}
                else if (key.equals("lever")) { SQL.append("And b.class = "+value+" ");}
                else if (key.equals("lowestPrice")) { SQL.append("And b.price >= "+value+" ");}
                else if (key.equals("highestPrice")) { SQL.append("And b.price <= "+value+" ");}
                else if (key.equals("managerName")) { SQL.append("And b.managerName like '%"+value+"%'");}
                else if (key.equals("managerPhone")) { SQL.append("And b.managerPhone like '%"+value+"%'");}
                else if (key.equals("staff_id")) { SQL.append("And ab.staff_id = "+value+" ");}
                else if (key.equals("buildingType")) { SQL.append("And bt.code in ("+value+") ");}
                else if (key.equals("smallestRentArea")) { SQL.append("And r.value >= "+value+" ");}
                else if (key.equals("largestRentArea")) { SQL.append("And r.value <= "+value+" ");}
                };
        });
        SQL.append("group by b.id");




        try(Connection connection = DriverManager.getConnection(URL, USER, PASS);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(SQL.toString())) {

            while (rs.next()){
                BuildingEntity buildingEntity = new BuildingEntity();
                buildingEntity.setName(rs.getString("name"));
                buildingEntity.setStreet(rs.getString("street"));
                buildingEntity.setWard(rs.getString("ward"));
                buildingEntity.setDistrictName(rs.getString("districtName"));
                buildingEntity.setNumberOfBasement(rs.getInt("numberOfBasement"));
                buildingEntity.setFloorArea(rs.getInt("floorArea"));
                buildingEntity.setPrice(rs.getInt("price"));
                buildingEntity.setServiceFee(rs.getString("serviceFee"));
                buildingEntity.setBrokerageFee(rs.getString("brokerageFee"));
                buildingEntity.setManageName(rs.getString("manageName"));
                buildingEntity.setManagePhone(rs.getString("managePhone"));
                String rentAreaStr = rs.getString("rentArea");
                if (rentAreaStr != null && !rentAreaStr.equals(""))
                {
                    String[] parts = rentAreaStr.split(",");
                    List<Integer> areas = new ArrayList<>();
                    for ( String part : parts)
                        try{
                            areas.add(Integer.parseInt(part.trim()));
                        }catch (NumberFormatException e){};
                    buildingEntity.setRentArea(areas);
                }
                results.add(buildingEntity);
            };
        }
        catch(Exception e){
            e.printStackTrace();
        };
        return results;
    }
}
