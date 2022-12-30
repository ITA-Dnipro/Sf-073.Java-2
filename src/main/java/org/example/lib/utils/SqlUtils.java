package org.example.lib.utils;

import java.sql.*;
import java.util.*;

public class SqlUtils {

    private SqlUtils() {
    }

    public static String saveQuery(Object o, Class<?> cls, Connection connection) throws SQLException {
        return "INSERT INTO " +
                EntityUtils.getTableName(cls) +
                " (" +
                getColumnNamesInsert(EntityUtils.getTableName(cls), connection) +
                ") " +
                "VALUES" +
                " (" +
                generatePlaceholdersForSave(getResultSetMetaData(EntityUtils.getTableName(cls), connection)) +
                ")";
    }

    public static String findByIdQuery(Long id, Class<?> cls) {
        return "SELECT * FROM " +
                EntityUtils.getTableName(cls) +
                "WHERE" +
                " id = " + id;
    }

    public static ResultSetMetaData getResultSetMetaData(String tableName, Connection connection) throws SQLException {
        String query = "SELECT * FROM " + tableName + " LIMIT 1";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        return resultSet.getMetaData();
    }

    public static String getColumnNamesInsert(String tableName, Connection connection) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        int columnCount = getResultSetMetaData(tableName, connection).getColumnCount();
        for (int i = 2; i <= columnCount; i++) {
            columnNames.add(getResultSetMetaData(tableName, connection).getColumnName(i));
        }
        return String.join(", ", columnNames);
    }

    public static String getColumnNamesUpdate(String tableName, Connection connection) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        int columnCount = getResultSetMetaData(tableName, connection).getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(getResultSetMetaData(tableName, connection).getColumnName(i));
        }
        return String.join(", ", columnNames);
    }

    public static String generatePlaceholdersForSave(ResultSetMetaData resultSetMetaData) throws SQLException {
        int columnCount = resultSetMetaData.getColumnCount() - 1;
        var sb = new StringBuilder();
        sb.append(" ?,".repeat(Math.max(0, columnCount)));
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
