package com.javaweb.repository.impl;

import com.javaweb.builder.BuildingSearchBuilder;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.entity.BuildingEntity;
import com.javaweb.repository.entity.DistrictEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
@Primary
public class JDBCBuildingRepositoryImpl implements BuildingRepository {

    @Autowired
    private DataSource dataSource;

    public static void JoinTable(BuildingSearchBuilder buildingSearchBuilder, StringBuilder SQL)
    {
        Integer staff_id = buildingSearchBuilder.getStaffId();
        if(staff_id!=null)
        {
            SQL.append(" inner join assignment_building ab on b.id = ab.building_id ");
        }
        Integer district_id = buildingSearchBuilder.getDistrictId();
        if(district_id!=null)
        {
            SQL.append(" inner join district d on b.district_id = d.id ");
        }
        List<String> typeCode = buildingSearchBuilder.getTypeCode();
        if(typeCode!= null && typeCode.size()!=0){
            SQL.append(" inner join building_building_type bbt on bbt.building_id=b.id inner join building_type bt on bt.id=bbt.building_type_id ");
        }
        Long rentAreaFrom = buildingSearchBuilder.getRentAreaFrom();
        Long rentAreaTo = buildingSearchBuilder.getRentAreaTo();
        if(rentAreaFrom!=null || rentAreaTo!=null)
        {
            SQL.append(" left join rent_area r on r.building_id = b.id ");
        }
    };

    public static void queryNomad(BuildingSearchBuilder buildingSearchBuilder, StringBuilder where){
//      for( Map.Entry<String, Object> it : param.entrySet()){
//        if(
//                !it.getKey().equals("staff_id") && !it.getKey().equals("district_id") && !it.getKey().startsWith("rentArea") && !it.getKey().startsWith("typeCode")
//        ){
//            if (it.getKey().equals("name")){ where.append("And b.name like '%"+it.getValue()+"%' ");}
//            else if (it.getKey().equals("ward")) { where.append("And b.ward = "+it.getValue()+" ");}
//            else if (it.getKey().equals("street")) { where.append("And b.street like '%"+it.getValue()+"%' ");}
//            else if (it.getKey().equals("floorArea")) { where.append("And b.floorArea = "+it.getValue()+" ");}
//            else if (it.getKey().equals("numberOfBasement")) { where.append("And b.numberOfBasement = "+it.getValue()+" ");}
//            else if (it.getKey().equals("direction")) { where.append("And b.direction = "+it.getValue()+" ");}
//            else if (it.getKey().equals("lever")) { where.append("And b.class = "+it.getValue()+" ");}
//            else if (it.getKey().equals("managerName")) { where.append("And b.managerName like '%"+it.getValue()+"%'");}
//            else if (it.getKey().equals("managerPhone")) { where.append("And b.managerPhone like '%"+it.getValue()+"%'");}
//            else if (it.getKey().startsWith("priceFrom")) { where.append("And b.price >= "+it.getValue()+" ");}
//            else if (it.getKey().startsWith("priceTo")) { where.append("And b.price <= "+it.getValue()+" ");}
//        }
//      }
        List<String> excludeFields = List.of("staffId", "districtId", "typeCode", "rentAreaFrom", "rentAreaFrom", "rentAreaTo", "priceFrom", "priceTo");
        try {
            Field[] fields = BuildingSearchBuilder.class.getDeclaredFields();
            for (Field item : fields){
                item.setAccessible(true);
                String fieldName = item.getName();
                // Bỏ qua các field đặc biệt được xử lý trong querySpecial / JoinTable
                if (excludeFields.contains(fieldName)) continue;

                Object value = item.get(buildingSearchBuilder);
                if (value != null) {
                    // field "level" map tới cột "class" trong DB (class là từ khoá SQL → cần backtick)
                    if (fieldName.equals("level")) {
                        where.append(" and b.`class` = '" + value + "' ");
                    } else if (item.getType() == Long.class || item.getType() == Integer.class) {
                        where.append(" and b." + fieldName + " = " + value +"");
                    } else {
                        where.append(" and b." + fieldName + " like '%" + value + "%' ");
                    }
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    };

    public static void querySpecial(BuildingSearchBuilder buildingSearchBuilder, StringBuilder where){
//        for( Map.Entry<String, Object> it : param.entrySet()){
//                if (it.getKey().equals("district_id")){ where.append("And b.district_id = "+it.getValue()+" ");}
//                else if (it.getKey().equals("staff_id")) { where.append("And ab.staff_id = "+it.getValue()+" ");}
//                else if (it.getKey().startsWith("typeCode")) { where.append("And bt.code in ("+it.getValue()+") ");}
//                else if (it.getKey().startsWith("rentAreaFrom")) { where.append("And r.value >= "+it.getValue()+" ");}
//                else if (it.getKey().startsWith("rentAreaTo")) { where.append("And r.value <= "+it.getValue()+" ");}
//        }
        Integer staffId = buildingSearchBuilder.getStaffId();
        if (staffId != null) {
            where.append(" and ab.staff_id = " + staffId+ " ");
        }
        Integer districtId = buildingSearchBuilder.getDistrictId();
        if (districtId != null) {
            where.append(" and b.district_id = " + districtId+" ");
        }
        Long rentAreaFrom = buildingSearchBuilder.getRentAreaFrom();
        Long rentAreaTo = buildingSearchBuilder.getRentAreaTo();
        if (rentAreaFrom != null || rentAreaTo != null) {
            where.append(" and exists (select * from rent_area r where b.id = r.building_id ");
            if (rentAreaFrom != null) {
                where.append(" and r.value >= " + rentAreaFrom+" ");
            }
            if (rentAreaTo != null) {
                where.append(" and r.value <= " + rentAreaTo+" ");
            }
            where.append(") ");
        }
        Integer priceFrom = buildingSearchBuilder.getPriceFrom();
        Integer priceTo = buildingSearchBuilder.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            if (priceFrom != null) {
                where.append(" and b.price >= " + priceFrom+" ");
            }
            if (priceTo != null) {
                where.append(" and b.price <= " + priceTo+" ");
            }
        }
        List<String> typeCode = buildingSearchBuilder.getTypeCode();
        if (typeCode != null && typeCode.size() != 0) {
            where.append(" and bt.code in (" + String.join(",", typeCode.stream().map(t -> "'" + t + "'").toArray(String[]::new)) + ") ");
        }
    };


    @Override
    public List<BuildingEntity> findAll(BuildingSearchBuilder buildingSearchBuilder) {
        List<BuildingEntity> results = new ArrayList<>();
        StringBuilder SQL= new StringBuilder(" select distinct b.* ");
        SQL.append(" from building b ");
        JDBCBuildingRepositoryImpl.JoinTable(buildingSearchBuilder,SQL);
        StringBuilder where = new StringBuilder(" Where 1=1 ");
        JDBCBuildingRepositoryImpl.queryNomad(buildingSearchBuilder,where);
        JDBCBuildingRepositoryImpl.querySpecial(buildingSearchBuilder,where);
        SQL.append(where);
        SQL.append(" group by b.id ");

        try(Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(SQL.toString())) {

            while (rs.next()){
                BuildingEntity buildingEntity = new BuildingEntity();
                DistrictEntity districtEntity = new DistrictEntity();
                buildingEntity.setId(rs.getInt("id"));
                buildingEntity.setName(rs.getString("name"));
                buildingEntity.setStreet(rs.getString("street"));
                buildingEntity.setWard(rs.getString("ward"));
                districtEntity.setId(rs.getInt("district_id"));
                buildingEntity.setDistrict(districtEntity);
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
