package com.javaweb.repository.impl;

import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.entity.DistrictEntity;
import com.javaweb.repository.entity.RentAreaEntity;
import com.javaweb.utils.ConnectionUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RentAreaRepositoryImpl implements RentAreaRepository {
    public List <RentAreaEntity> getRentAreaByBuildingID(Integer building_id){
        List<RentAreaEntity> result = new ArrayList<>();
        String SQL = "select ra.value from rent_area ra where ra.building_id="+building_id+" ";
        try(Connection connection = ConnectionUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(SQL.toString())) {

            while (rs.next()){
                RentAreaEntity rentAreaEntity = new RentAreaEntity();
                rentAreaEntity.setValue(rs.getLong("value"));
                result.add(rentAreaEntity);
            };

        }
        catch(Exception e){
            e.printStackTrace();
        };
        return result;
    };
}