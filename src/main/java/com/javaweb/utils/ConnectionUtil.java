package com.javaweb.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionUtil {
    final static String URL = "jdbc:postgresql://localhost:5432/Real_estate_management_dev";
    final static String USER = "postgres";
    final static String PASS = "change-me-local";

    public static Connection getConnection() throws SQLException{
        Connection connection = null;
        connection = DriverManager.getConnection(URL, USER, PASS);
        return connection;
    }
}
