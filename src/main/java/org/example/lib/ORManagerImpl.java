package org.example.lib;

import org.example.entity.Publisher;
import org.example.lib.annotation.*;
import org.example.lib.exception.ExistingObjectException;
import org.example.lib.exception.ORMException;
import org.example.lib.exception.UnsupportedTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.lib.utils.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class ORManagerImpl implements ORManager {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    private static final String ID = " BIGINT PRIMARY KEY AUTO_INCREMENT";
    private static final String NAME = " VARCHAR(255) UNIQUE NOT NULL";
    private static final String DATE = " DATE NOT NULL";
    private static final String LONG = " BIGINT NOT NULL";
    private static final String FIND_ALL = "SELECT * FROM ";
    private static final String FOREIGN_KEY = " FOREIGN KEY (%s) REFERENCES %s(%s)";

    private final Connection connection;
    private final static Logger LOGGER = LoggerFactory.getLogger(ORManagerImpl.class);

    public ORManagerImpl(DataSource dataSource) throws SQLException {
        this.connection = dataSource.getConnection();
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public void register(Class<?>... entityClasses) throws ORMException {
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
                    } else if (field.isAnnotationPresent(ManyToOne.class)) {
                        String fieldName = EntityUtils.getFieldName(field);

                        sql.add(fieldName + " BIGINT");
                        sql.add(String.format(FOREIGN_KEY, fieldName, field.getName() + "s", "id"));
                    }
                }

                String sqlCreateTable = String.format("%s %s(%s);", CREATE_TABLE, tableName,
                        String.join(", ", sql));

                try (var prepStmt = getConnection().prepareStatement(sqlCreateTable)) {
                    prepStmt.executeUpdate();
                } catch (SQLException exception) {
                    LOGGER.error("Cannot create table without annotation @Entity or fields without annotation @Id, @Column or @ManyToOne");
                    throw new ORMException("An error has occurred");
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
            sql.add(name + LONG);
        }
    }

    private String getFieldName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            String name = field.getAnnotation(Column.class).name();
            if (!name.equals("")) {
                return name;
            }
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            String name = field.getAnnotation(ManyToOne.class).columnName();
            if (!name.equals("")) {
                return name;
            }
        }
        return field.getName();
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
        long id = 0L;
        T newRecord = null;
        Map<String, Object> associatedEntities = new HashMap<>();
        if(!EntityUtils.hasId(o)){
            try(PreparedStatement statement = connection.prepareStatement(SqlUtils.saveQuery(o, o.getClass(), connection),
                    Statement.RETURN_GENERATED_KEYS);
                PreparedStatement stBefore = connection.prepareStatement(SqlUtils.selectFirstFromTable(o.getClass()))) {
                ResultSetMetaData resultSetMetaData = stBefore.getMetaData();
                EntityUtils.setterPreparedStatementExecution(statement, resultSetMetaData, o, associatedEntities);
                statement.executeUpdate();
                ResultSet keys = statement.getGeneratedKeys();
                while (keys.next()) {
                    id = keys.getLong("id");
                }
                Optional<T> optRecord = (Optional<T>) Optional.ofNullable(findById(id, o.getClass())).orElseThrow();
                if(optRecord.isPresent()){
                    newRecord = (T) optRecord.get();
                    EntityUtils.addNewRecordToAssociatedManyToOneCollection(newRecord, resultSetMetaData, associatedEntities);
                }
            }catch(SQLException ex){
                ex.printStackTrace();
            }
        }else{
            long entityId = EntityUtils.getId(o);
            Optional<T> optRecord = (Optional<T>) Optional.ofNullable(findById(entityId, o.getClass())).orElseThrow();
            if(optRecord.isPresent()){
                newRecord = (T) optRecord.get();
            }
        }
        return newRecord;
    }

    @Override
    public void persist(Object o) throws ORMException, ExistingObjectException {
        String tableName = getTableName(o.getClass());
        String fieldList = getFieldsWithoutId(o);
        String valueList = getValues(o);

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);

        try {
            this.connection.prepareStatement(sql).execute();
            LOGGER.info(String.format("Successfully added %s to the database", o.getClass()));
        } catch (SQLException exception) {
            LOGGER.error(String.format("%s with that name already exists in the database. The name of the %s should be UNIQUE", o.getClass(), o.getClass()));
            throw new ExistingObjectException("An error has occurred");
        }
    }

    private String getValues(Object o) throws ORMException {
        Field[] declaredFields = o.getClass().getDeclaredFields();

        List<String> result = new ArrayList<>();
        try {
            for (Field declaredField : declaredFields) {
                if (declaredField.getAnnotation(Column.class) != null) {
                    declaredField.setAccessible(true);
                    Object value = declaredField.get(o);
                    result.add("'" + value.toString() + "'");
                }
            }
        } catch (IllegalAccessException exception) {
            LOGGER.debug("Cannot get the values the entity");
            throw new ORMException("An error has occurred");
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
    public <T> Optional<T> findById(Serializable id, Class<T> cls) throws ORMException {
        String tableName = getTableName(cls);
        String sql = String.format("SELECT * FROM %s WHERE id = %s;", tableName, id);

        ResultSet resultSet;
        try {
            resultSet = connection.prepareStatement(sql).executeQuery();
        } catch (SQLException exception) {
            throw new ORMException(exception.getMessage());
        }

        return createEntity(cls, resultSet);

    }

    private <T> Optional<T> createEntity(Class<T> cls, ResultSet resultSet) throws ORMException {

        try {
            if (!resultSet.next()) {
                LOGGER.info("Element with that ID doesnt exist");
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new ORMException("An error has occurred");
        }

        String fieldName;
        T entity;
        try {
            entity = cls.getDeclaredConstructor().newInstance();

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
        } catch (NoSuchMethodException | IllegalAccessException | SQLException | InvocationTargetException |
                 InstantiationException exception) {
            LOGGER.error("Cannot create entity");
            throw new ORMException(exception.getMessage());
        }
        return Optional.of(entity);
    }

    private <T> T fillData(T entity, Field field, String value) {
        field.setAccessible(true);
        try {
            if (field.getType() == long.class || field.getType() == Long.class) {
                field.set(entity, Long.parseLong(value));
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                field.set(entity, Integer.parseInt(value));
            } else if (field.getType() == LocalDate.class) {
                field.set(entity, LocalDate.parse(value));
            } else if (field.getType() == String.class) {
                field.set(entity, value);

            }
        } catch (IllegalAccessException exception) {
            LOGGER.error(String.format("Unsupported type %s", field.getType()));
            throw new UnsupportedTypeException("An error has occurred");
        }
        return entity;
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) throws ORMException {
        List<T> result = new ArrayList<>();
        String sql = FIND_ALL + getTableName(cls) + ";";

        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                T obj = cls.getConstructor().newInstance();
                Field[] declaredFields = obj.getClass().getDeclaredFields();
                for (Field field : declaredFields) {
                    if (!field.isAnnotationPresent(Id.class) && !field.isAnnotationPresent(Column.class)) {
                        continue;
                    }
                    String name = getFieldName(field);
                    String value = resultSet.getString(name);
                    field.setAccessible(true);
                    fillData(obj, field, value);
                }
                result.add(obj);
            }
        } catch (NoSuchMethodException | IllegalAccessException | SQLException | InvocationTargetException |
                 InstantiationException exception) {
            LOGGER.error("Something went wrong");
            throw new ORMException(exception.getMessage());
        }
        return result;
    }

    @Override
    public <T> Iterable<T> findAllAsIterable(Class<T> cls) throws Exception {
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
