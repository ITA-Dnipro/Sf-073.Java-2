package org.example.lib;

import org.example.lib.annotation.Column;
import org.example.lib.annotation.Entity;
import org.example.lib.annotation.Id;
import org.example.lib.annotation.Table;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ORManagerImpl implements ORManager {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    private static final String ID = " BIGINT PRIMARY KEY AUTO_INCREMENT";
    private static final String NAME = " VARCHAR(255) UNIQUE NOT NULL";
    private static final String DATE = " DATE NOT NULL";
    private static final String INT = " INT NOT NULL";

    private final Connection connection;

    public ORManagerImpl(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public void register(Class<?>... entityClasses) throws SQLException, IllegalAccessException {
        for (Class<?> cls : entityClasses) {

            if (cls.isAnnotationPresent(Entity.class)) {
                String tableName = getTableName(cls);

                Field[] declaredFields = cls.getDeclaredFields();
                ArrayList<String> sql = new ArrayList<>();

                for (Field field : declaredFields) {
                    Class<?> fieldType = field.getType();
                    if (field.isAnnotationPresent(Id.class)) {
                        String name = field.getName();
                        getColumnName(sql, fieldType, name);
                    } else if (field.isAnnotationPresent(Column.class)) {
                        String name = getFieldName(field);
                        getColumnName(sql, fieldType, name);
                    }
                }

                String sqlCreateTable = String.format("%s %s(%s);", CREATE_TABLE, tableName,
                        String.join(", ", sql));

                try (var prepStmt = getConnection().prepareStatement(sqlCreateTable)) {
                    prepStmt.executeUpdate();
                }
            }
        }
    }


    private void getColumnName(ArrayList<String> sql, Class<?> type, String name) {
        if (type == Long.class) {
            sql.add(name + ID);
        }
        if (type == String.class) {
            sql.add(name + NAME);
        } else if (type == LocalDate.class) {
            sql.add(name + DATE);
        } else if (type == int.class) {
            sql.add(name + INT);
        }
    }

    private String getFieldName(Field field) {
        String name = field.getAnnotation(Column.class).name();
        if (name.equals("")) {
            name = field.getName();
        }
        return name;
    }

    public static String getTableName(Class<?> cls) {
        if (cls.isAnnotationPresent(Table.class)) {
            String name = cls.getAnnotation(Table.class).name();
            if (!name.equals("")) {
                return name;
            }
        }
        return cls.getSimpleName();
    }

    @Override
    public <T> T save(T o) throws SQLException {
        return null;
    }

    @Override
    public void persist(Object o) throws SQLException, IllegalAccessException {
        String tableName = getTableName(o.getClass());
        String fieldList = getFieldsWithoutId(o);
        String valueList = getValues(o);

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);

        this.connection.prepareStatement(sql).execute();
    }

    private String getValues(Object o) throws IllegalAccessException {
        Field[] declaredFields = o.getClass().getDeclaredFields();

        List<String> result = new ArrayList<>();

        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(Column.class) != null) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(o);
                result.add("'" + value.toString() + "'");
            }
        }
        return String.join(",", result);
    }

    private String getFieldsWithoutId(Object o) {
        return Arrays.stream(o.getClass()
                        .getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Column.class) != null)
                .map(f -> {
                    String name = f.getAnnotation(Column.class).name();
                    if (!name.equals("")) {
                        return name;
                    } else {
                        return f.getName();
                    }
                })
                .collect(Collectors.joining(","));
    }

    @Override
    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        return Optional.empty();
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return null;
    }

    @Override
    public <T> Iterable<T> findAllAsIterable(Class<T> cls) {
        return null;
    }

    @Override
    public <T> Stream<T> findAllAsStream(Class<T> cls) {
        return null;
    }

    @Override
    public <T> T merge(T o) {
        return null;
    }

    @Override
    public <T> T refresh(T o) {
        return null;
    }

    @Override
    public boolean delete(Object o) {
        return false;
    }
}
