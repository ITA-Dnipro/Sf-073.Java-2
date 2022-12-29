package org.example.lib;

import org.example.lib.annotation.*;
import org.example.lib.utils.*;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class ORManagerImpl implements ORManager {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    private static final String ID = " BIGINT PRIMARY KEY AUTO_INCREMENT";
    private static final String NAME = " VARCHAR(255) UNIQUE NOT NULL";
    private static final String DATE = " DATE NOT NULL";
    private static final String INT = " INT NOT NULL";
    private static final String FIND_ALL = "SELECT * FROM ";

    private final Connection connection;

    public ORManagerImpl(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public void register(Class<?>... entityClasses) throws Exception {
        for (Class<?> cls : entityClasses) {

            if (cls.isAnnotationPresent(Entity.class)) {
                String tableName = EntityUtils.getTableName(cls);

                Field[] declaredFields = cls.getDeclaredFields();
                ArrayList<String> sql = new ArrayList<>();

                for (Field field : declaredFields) {
                    Class<?> fieldType = field.getType();
                    if (field.isAnnotationPresent(Id.class)) {
                        setColumnType(sql, fieldType, EntityUtils.getFieldName(field));
                    } else if (field.isAnnotationPresent(Column.class)) {
                        setColumnType(sql, fieldType, EntityUtils.getFieldName(field));
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


    private void setColumnType(ArrayList<String> sql, Class<?> type, String name) {
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


    @Override
    public <T> T save(T o) {
        long id = 0L;
        T newRecord = null;
        if(!EntityUtils.hasId(o)){
            try (PreparedStatement statement = connection.prepareStatement(SqlUtils.saveQuery(o, o.getClass(), connection),
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                ResultSet keys = statement.getGeneratedKeys();
                while(keys.next()){
                    id = keys.getLong("id");
                }
                try (PreparedStatement statement1 = connection.prepareStatement(SqlUtils.findByIdQuery(id, o.getClass()))) {
                    ResultSet resultSet = statement1.executeQuery();
                    while(resultSet.next()){
                    Optional<?> optionalRecord = findById(id, o.getClass());
                    newRecord = (T) optionalRecord.orElse(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (SQLException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }else{
            long entityId = EntityUtils.getId(o);
            //TODO implement insert with update
        }
        return newRecord;
    }

    @Override
    public void persist(Object o) throws SQLException, IllegalAccessException {
        String tableName = EntityUtils.getTableName(o.getClass());
        String fieldList = EntityUtils.getFieldsWithoutId(o);
        String valueList = EntityUtils.getFieldValues(o);

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);

        this.connection.prepareStatement(sql).execute();
    }


    @Override
    public <T> Optional<T> findById(Serializable id, Class<T> cls) throws Exception{

        String tableName = EntityUtils.getTableName(cls);

        String sql = String.format("SELECT * FROM %s WHERE id = %s;", tableName, id);

        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();

        return createEntity(cls, resultSet);

    }

    private <T> Optional<T> createEntity(Class<T> cls, ResultSet resultSet) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if (!resultSet.next()) {
            return Optional.empty();
        }

        String fieldName;

        T entity = cls.getDeclaredConstructor().newInstance();

        Field[] declaredFields = cls.getDeclaredFields();
        for (Field declaredField : declaredFields) {

            if (!declaredField.isAnnotationPresent(Column.class) &&
                    !declaredField.isAnnotationPresent(Id.class)) {
                continue;
            }
            Column columnAnnotation = declaredField.getAnnotation(Column.class);

            if (columnAnnotation == null) {
                fieldName = declaredField.getName();
            } else if (!columnAnnotation.name().equals("")) {
                fieldName = columnAnnotation.name();
            } else {
                fieldName = declaredField.getName();
            }

            String value = resultSet.getString(fieldName);
            entity = fillData(entity, declaredField, value);
        }
        return Optional.of(entity);
    }

    private <T> T fillData(T entity, Field field, String value) throws IllegalAccessException {
        field.setAccessible(true);

        if (field.getType() == long.class || field.getType() == Long.class) {
            field.set(entity, Long.parseLong(value));

        } else if (field.getType() == int.class || field.getType() == Integer.class) {
            field.set(entity, Integer.parseInt(value));

        } else if (field.getType() == LocalDate.class) {
            field.set(entity, LocalDate.parse(value));

        } else if (field.getType() == String.class) {
            field.set(entity, value);

        } else {
            throw new RuntimeException("Unsupported type " + field.getType());
        }
        return entity;
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) throws Exception {
        List<T> result = new ArrayList<>();

        String sql = FIND_ALL + cls.getSimpleName() + ";";
        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {

            T obj = cls.getConstructor().newInstance();

            Field[] declaredFields = obj.getClass().getDeclaredFields();

            for (Field field : declaredFields) {

                if (!field.isAnnotationPresent(Id.class) && !field.isAnnotationPresent(Column.class)) {
                    continue;
                }

                String name = EntityUtils.getFieldName(field);
                String value = resultSet.getString(name);

                field.setAccessible(true);
                setFieldValue(obj, field, value);
            }
            result.add(obj);
        }
        return result;
    }

    private <T> void setFieldValue(T obj, Field field, String value) throws IllegalAccessException {
        if (field.getType() == Long.class) {
            field.set(obj, Long.parseLong(value));
        } else if (field.getType() == String.class) {
            field.set(obj, value);
        } else if (field.getType() == LocalDate.class) {
            field.set(obj, LocalDate.parse(value));
        }
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
