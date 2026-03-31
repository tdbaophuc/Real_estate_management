package com.javaweb.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionUtil {
    final static String URL = "jdbc:mysql://localhost:3306/java_backend_project01";
    final static String USER = "root";
    final static String PASS = "123456";
    public static Connection getConnection() throws SQLException{
        Connection connection = null;
        connection = DriverManager.getConnection(URL, USER, PASS);
        return connection;
    }
}
