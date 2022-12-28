package org.example.lib.utils;

import java.sql.*;
import java.util.*;

public class SqlUtils {

    private SqlUtils() {}

    public static String saveQuery(Object o, Class<?> cls, Connection connection) throws IllegalAccessException {
        return "INSERT INTO " +
                EntityUtils.getTableName(cls) +
                " ( " +
                getColumnNamesInsert(EntityUtils.getTableName(cls), connection) +
                " ) " +
                " VALUES " +
                " ( " +
                EntityUtils.getFieldValues(o) +
                " ) ";
    }

    public static String findByIdQuery(Long id, Class<?> cls){
        return "SELECT * FROM " +
                EntityUtils.getTableName(cls) +
                " WHERE " +
                " id = " + id;
    }

    public static String getColumnNamesInsert(String tableName, Connection connection) {
        List<String> columnNames = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " LIMIT 1";
        try (Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnsNumber = resultSetMetaData.getColumnCount();
            for (int i = 2; i <= columnsNumber; i++) {
                columnNames.add(resultSetMetaData.getColumnName(i));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return String.join(", ", columnNames);
    }

    public static String getColumnNamesUpdate(String tableName, Connection connection) {
        List<String> columnNames = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " LIMIT 1";
        try (Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnsNumber = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnsNumber; i++) {
                columnNames.add(resultSetMetaData.getColumnName(i));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return String.join(", ", columnNames);
    }
}
