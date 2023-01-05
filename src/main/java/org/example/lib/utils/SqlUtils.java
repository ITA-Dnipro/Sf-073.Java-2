package org.example.lib.utils;

import java.sql.*;
import java.util.*;

public class SqlUtils {

    private static final String SELECT_ALL_FROM = "SELECT * FROM ";

    private SqlUtils() {
    }

    public static String saveQuery(Object o, Connection connection) throws SQLException {
        return "INSERT INTO " +
                EntityUtils.getTableName(o.getClass()) +
                " (" +
                getColumnNamesInsert(EntityUtils.getTableName(o.getClass()), connection) +
                ") " +
                "VALUES" +
                " (" +
                generatePlaceholdersForSave(getResultSetMetaData(EntityUtils.getTableName(o.getClass()), connection)) +
                ")";
    }

    public static String updateQuery(Object o, Connection connection) throws SQLException {
        return "UPDATE " +
                EntityUtils.getTableName(o.getClass()) +
                " SET " +
                generateColumnNamesWithPlaceholdersForUpdate(EntityUtils.getTableName(o.getClass()), connection) +
                " WHERE " +
                EntityUtils.getTableName(o.getClass()) + "." + EntityUtils.getIdFieldName(o.getClass()) +
                " = " +
                EntityUtils.getId(o);
    }

    public static String findByIdQuery(Object o) {
        return SELECT_ALL_FROM +
                EntityUtils.getTableName(o.getClass()) +
                " WHERE " +
                EntityUtils.getTableName(o.getClass()) + "." + EntityUtils.getIdFieldName(o.getClass()) +
                " = " +
                EntityUtils.getId(o);
    }

    public static String selectFirstFromTable(Class<?> cls) {
        return SELECT_ALL_FROM +
                EntityUtils.getTableName(cls) +
                " LIMIT 1";
    }

    public static ResultSetMetaData getResultSetMetaData(String tableName, Connection connection) throws SQLException {
        String query = SELECT_ALL_FROM + tableName + " LIMIT 1";
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

    public static List<String> getColumnNamesUpdate(String tableName, Connection connection) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        int columnCount = getResultSetMetaData(tableName, connection).getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(getResultSetMetaData(tableName, connection).getColumnName(i));
        }
        return columnNames;
    }

    public static String generatePlaceholdersForSave(ResultSetMetaData resultSetMetaData) throws SQLException {
        int columnCount = resultSetMetaData.getColumnCount() - 1;
        var sb = new StringBuilder();
        sb.append(" ?,".repeat(Math.max(0, columnCount)));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String generateColumnNamesWithPlaceholdersForUpdate(String tableName, Connection connection) throws SQLException {

        var sb = new StringBuilder();
        for (int i = 1; i < getColumnNamesUpdate(tableName, connection).size(); i++) {
            sb.append(getColumnNamesUpdate(tableName, connection).get(i)).append(" = ?, ");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();

    }
}
