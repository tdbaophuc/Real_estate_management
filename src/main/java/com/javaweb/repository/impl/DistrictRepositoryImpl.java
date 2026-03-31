package com.javaweb.repository.impl;

import com.javaweb.repository.DistrictRepository;
import com.javaweb.repository.entity.DistrictEntity;
import com.javaweb.utils.ConnectionUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DistrictRepositoryImpl implements DistrictRepository {
    @Override
    public DistrictEntity findNameById(Integer id) {
        DistrictEntity districtEntity = new DistrictEntity();
        String SQL = "select d.name from district d where d.id="+id+" ";
        try(Connection connection = ConnectionUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(SQL.toString())) {

            while (rs.next()){
                districtEntity.setName(rs.getString("name"));
            };
        }
        catch(Exception e){
            e.printStackTrace();
        };
        return districtEntity;
    }
}
