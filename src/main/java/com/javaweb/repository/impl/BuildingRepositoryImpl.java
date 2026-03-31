package com.javaweb.repository.impl;

import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.utils.ConnectionUtil;
import com.javaweb.utils.StringUtil;
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

    public static void JoinTable(Map<String, String> param, List<String>typeCode, StringBuilder SQL)
    {
        String staff_id = (String)param.get("staff_id");
        if(StringUtil.checkString(staff_id))
        {
            SQL.append("inner join assignment_building ab on b.id = ab.building_id ");
        }
        String district_id = (String)param.get("district_id");
        if(StringUtil.checkString(district_id))
        {
            SQL.append("inner join district d on b.district_id = d.id ");
        }
        if(typeCode!= null && typeCode.size()!=0){
            SQL.append("inner join building_building_type bbt on bbt.building_id=b.id inner join building_type bt on bt.id=bbt.building_type_id ");
        }
        String rentAreaFrom = (String)param.get("rentAreaFrom");
        String rentAreaTo = (String)param.get("rentAreaTo");
        if(StringUtil.checkString(rentAreaFrom) || StringUtil.checkString(rentAreaTo))
        {
            SQL.append("left join rent_area r on r.building_id = b.id ");
        }
    };

    public static void queryNomad(Map<String, String> param, StringBuilder where){
      for( Map.Entry<String, String> it : param.entrySet()){
        if(
                !it.getKey().equals("staff_id") && !it.getKey().equals("district_id") && !it.getKey().startsWith("rentArea") && !it.getKey().startsWith("typeCode")
        ){
            if (it.getKey().equals("name")){ where.append("And b.name like '%"+it.getValue()+"%' ");}
            else if (it.getKey().equals("ward")) { where.append("And b.ward = "+it.getValue()+" ");}
            else if (it.getKey().equals("street")) { where.append("And b.street like '%"+it.getValue()+"%' ");}
            else if (it.getKey().equals("floorArea")) { where.append("And b.floorArea = "+it.getValue()+" ");}
            else if (it.getKey().equals("numberOfBasement")) { where.append("And b.numberOfBasement = "+it.getValue()+" ");}
            else if (it.getKey().equals("direction")) { where.append("And b.direction = "+it.getValue()+" ");}
            else if (it.getKey().equals("lever")) { where.append("And b.class = "+it.getValue()+" ");}
            else if (it.getKey().equals("managerName")) { where.append("And b.managerName like '%"+it.getValue()+"%'");}
            else if (it.getKey().equals("managerPhone")) { where.append("And b.managerPhone like '%"+it.getValue()+"%'");}
            else if (it.getKey().startsWith("priceFrom")) { where.append("And b.price >= "+it.getValue()+" ");}
            else if (it.getKey().startsWith("priceTo")) { where.append("And b.price <= "+it.getValue()+" ");}
        }
      }
    };

    public static void querySpecial(Map<String, String> param, List<String> typeCode, StringBuilder where){
        for( Map.Entry<String, String> it : param.entrySet()){
                if (it.getKey().equals("district_id")){ where.append("And b.district_id = "+it.getValue()+" ");}
                else if (it.getKey().equals("staff_id")) { where.append("And ab.staff_id = "+it.getValue()+" ");}
                else if (it.getKey().startsWith("typeCode")) { where.append("And bt.code in ("+it.getValue()+") ");}
                else if (it.getKey().startsWith("rentAreaFrom")) { where.append("And r.value >= "+it.getValue()+" ");}
                else if (it.getKey().startsWith("rentAreaTo")) { where.append("And r.value <= "+it.getValue()+" ");}
        }
    };


    @Override
    public List<BuildingEntity> findAll(Map<String, String> param, List<String>typeCode) {
        List<BuildingEntity> results = new ArrayList<>();
        StringBuilder SQL= new StringBuilder("select distinct b.* ");
        SQL.append("from building b ");
        BuildingRepositoryImpl.JoinTable(param,typeCode,SQL);
        StringBuilder where = new StringBuilder(" Where 1=1 ");
        BuildingRepositoryImpl.queryNomad(param,where);
        BuildingRepositoryImpl.querySpecial(param,typeCode,where);
        SQL.append(where);
        SQL.append("group by b.id");

        try(Connection connection = ConnectionUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(SQL.toString())) {

            while (rs.next()){
                BuildingEntity buildingEntity = new BuildingEntity();
                buildingEntity.setId(rs.getInt("id"));
                buildingEntity.setName(rs.getString("name"));
                buildingEntity.setStreet(rs.getString("street"));
                buildingEntity.setWard(rs.getString("ward"));
                buildingEntity.setDistrictId(rs.getInt("district_id"));
                buildingEntity.setNumberOfBasement(rs.getInt("numberOfBasement"));
                buildingEntity.setFloorArea(rs.getInt("floorArea"));
                buildingEntity.setPrice(rs.getInt("price"));
                buildingEntity.setServiceFee(rs.getString("serviceFee"));
                buildingEntity.setBrokerageFee(rs.getString("brokerageFee"));
                buildingEntity.setManageName(rs.getString("manageName"));
                buildingEntity.setManagePhone(rs.getString("managePhone"));
                results.add(buildingEntity);
            };
        }
        catch(Exception e){
            e.printStackTrace();
        };
        return results;
    }
}
