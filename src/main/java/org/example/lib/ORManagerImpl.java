package org.example.lib;

import org.example.lib.annotation.*;
import org.example.lib.exception.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.lib.utils.*;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;


public class ORManagerImpl implements ORManager {
    private final Connection connection;
    private static final Logger LOGGER = LoggerFactory.getLogger(ORManagerImpl.class);

    public ORManagerImpl(DataSource dataSource) throws SQLException {
        this.connection = dataSource.getConnection();
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public void register(Class<?>... entityClasses) {
        for (Class<?> cls : entityClasses) {
            if (cls.isAnnotationPresent(Entity.class)) {
                String tableName = EntityUtils.getTableName(cls);
                Field[] declaredFields = cls.getDeclaredFields();
                ArrayList<String> sqlArray = new ArrayList<>();
                EntityUtils.buildSqlCreateTable(declaredFields, sqlArray);
                String sqlCreateTable = SqlUtils.createSqlTable(tableName, sqlArray);
                try (var prepStmt = getConnection().prepareStatement(sqlCreateTable)) {
                    prepStmt.executeUpdate();
                } catch (SQLException exception) {
                    LOGGER.error(
                            "Cannot create table without annotation @Entity or fields without annotation @Id, @Column or @ManyToOne",
                            new ORMException(exception.getMessage()));
                }
            }
        }
    }

    @Override
    public <T> T save(T o) throws ORMException, SQLException {
        Map<String, Object> associatedManyToOneEntities = new HashMap<>();
        Map<String, Object> associatedOneToManyEntities = new HashMap<>();
        Long id = null;
        if (!EntityUtils.hasId(o)) {
            try (PreparedStatement statement = connection.prepareStatement(SqlUtils.saveQuery(o, connection),
                                                                           Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement stBefore = connection.prepareStatement(
                         SqlUtils.selectFirstFromTable(o.getClass()))) {
                ResultSetMetaData resultSetMetaData = stBefore.getMetaData();
                EntityUtils.setterPreparedStatementExecution(statement, resultSetMetaData, o,
                                                             associatedManyToOneEntities);
                statement.executeUpdate();
                ResultSet keys = statement.getGeneratedKeys();
                keys.next();
                id = keys.getLong(EntityUtils.getIdFieldName(o.getClass()));
                EntityUtils.setFieldId(id, o);
            } catch (SQLException | IllegalAccessException | ClassNotFoundException ex) {
                if (ex.getClass().getTypeName().equals("org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException")) {
                    LOGGER.error(String.format(
                            "SQL exception: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException saving %s",
                            o.getClass()));
                    LOGGER.info(
                            "Possible reason: columns with key constraints NON NULL || UNIQUE prevent saving this record");
                    throw new ExistingObjectException(
                            "Please provide non existing entity or check for duplicate constraint fields");
                }
            }
            try (ResultSet rs = connection.prepareStatement(SqlUtils.findByIdQuery(id, o.getClass())).executeQuery()) {
                rs.next();
                EntityUtils.addNewRecordToAssociatedManyToOneCollection(o, rs, associatedManyToOneEntities);
                EntityUtils.saveNewRecordFromAssociatedOneToManyCollection(this, o);
                String successMessage = "Successfully SAVED" + o + "to the database";
                LOGGER.info(successMessage);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            o = merge(o);
        }
        return o;
    }

    @Override
    public void persist(Object o) throws ORMException {
        String tableName = EntityUtils.getTableName(o.getClass());
        String fieldList = EntityUtils.getFieldsWithoutId(o);
        String valueList = EntityUtils.getFieldValues(o);

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(sql,
                                                                                   Statement.RETURN_GENERATED_KEYS);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            while (resultSet.next()) {
                EntityUtils.entityIdGenerator(o, resultSet);
            }
            resultSet.close();
            LOGGER.info(String.format("Successfully added %s to the database", o.getClass().getSimpleName()));
        } catch (SQLException exception) {
            LOGGER.error(String.format(
                                 "%s with that name already exists in the database. The name of the %s should be UNIQUE"
                                 , o.getClass().getSimpleName(), o.getClass().getSimpleName()),
                         new ExistingObjectException(exception.getMessage()));
        }
    }

    @Override
    public <T> Optional<T> findById(Serializable id, Class<T> cls) throws ORMException {
        String tableName = EntityUtils.getTableName(cls);
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
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new ORMException(exception.getMessage());
        }

        String fieldName = null;
        T entity;
        try {
            entity = cls.getDeclaredConstructor().newInstance();

            Field[] declaredFields = cls.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (!declaredField.isAnnotationPresent(Column.class) &&
                        !declaredField.isAnnotationPresent(Id.class) &&
                        !declaredField.isAnnotationPresent(ManyToOne.class) &&
                        !declaredField.isAnnotationPresent(OneToMany.class)) {
                    continue;
                }

                if (declaredField.isAnnotationPresent(Column.class)) {
                    Column annotation = declaredField.getAnnotation(Column.class);
                    if (!annotation.name().equals("")) {
                        fieldName = annotation.name();
                    } else {
                        fieldName = declaredField.getName();
                    }
                } else if (declaredField.isAnnotationPresent(Id.class)) {
                    fieldName = declaredField.getName();
                } else if (declaredField.isAnnotationPresent(ManyToOne.class)) {
                    ManyToOne annotation = declaredField.getAnnotation(ManyToOne.class);
                    if (!annotation.columnName().equals("")) {
                        fieldName = annotation.columnName();
                    } else {
                        fieldName = declaredField.getName();
                    }
                }
                String value = resultSet.getString(fieldName);
                entity = mapValuesToObject(entity, declaredField, value);
            }
        } catch (NoSuchMethodException | IllegalAccessException | SQLException | InvocationTargetException |
                 InstantiationException exception) {
            LOGGER.error("Cannot create entity");
            throw new ORMException(exception.getMessage());
        }
        return Optional.of(entity);
    }

    private <T> T mapValuesToObject(T entity, Field field, String value) {
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
            } else if (field.getType().isAnnotationPresent(Entity.class)) {
                Optional<Object> optionalObj = findById(value, (Class<Object>) field.getType());
                optionalObj.ifPresent(o -> {
                    try {
                        field.set(entity, optionalObj.get());
                    } catch (Exception exception) {
                        LOGGER.error(String.format("There is no %s with this ID", o.getClass().getSimpleName()), new ORMException(exception.getMessage()));
                    }
                });
            }
        } catch (IllegalAccessException exception) {
            LOGGER.error(String.format("Unsupported type %s", field.getType()),
                    new UnsupportedTypeException(exception.getMessage()));
        } catch (ORMException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) throws ORMException {
        List<T> result = new ArrayList<>();
        String sql = SqlUtils.findAll() + EntityUtils.getTableName(cls) + ";";
        try (ResultSet resultSet = getConnection().prepareStatement(sql).executeQuery()) {
            while (resultSet.next()) {
                T obj = EntityUtils.mapObject(cls, resultSet, connection);
                result.add(obj);
            }
        } catch (SQLException exception) {
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
    public <T> T refresh(T o) throws ORMException {
        Object refreshed = null;
        Field idField = EntityUtils.getIdColumn(o.getClass());
        idField.setAccessible(true);
        Object objectId = EntityUtils.getFieldIdValue(o, idField);
        String sql = SqlUtils.findByIdQuery(Long.parseLong(objectId.toString()), o.getClass());
        try (ResultSet resultSet = getConnection().prepareStatement(sql).executeQuery()) {
            while (resultSet.next()) {
                refreshed = EntityUtils.mapObject(o.getClass(), resultSet, connection);
            }
        } catch (SQLException exception) {
            throw new ORMException(exception.getMessage());
        }
        o = (T) refreshed;
        return o;
    }

    @Override
    public boolean delete(Object o) throws ORMException {
        String tableName = EntityUtils.getTableName(o.getClass());
        Field idField = EntityUtils.getIdColumn(o.getClass());
        String idName = EntityUtils.getSQLColumName(idField);
        Object idValue = EntityUtils.getFieldIdValue(o, idField);

        dropConstraint(o, idValue);

        String deleteQuery = String.format("DELETE FROM %s WHERE %s = %s", tableName, idName, idValue);

        try {
            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
            deleteStatement.execute();
            if (idValue == null) {
                throw new SQLException();
            }
            LOGGER.info(String.format("Successfully deleted %s with Id %s", o.getClass().getSimpleName(), idValue));
        } catch (SQLException exception) {
            LOGGER.error("Cannot delete element with this ID");
        }
        return false;
    }

    private void dropConstraint(Object o, Object idValue) {


        Field[] declaredFields = o.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                String sqlDropFKQuery = String.format("DELETE FROM %s WHERE %s = %s;", field.getName().toUpperCase(),
                        o.getClass().getSimpleName().toLowerCase() + "_" + o.getClass().getDeclaredFields()[0].getName().toLowerCase(), idValue);
                try {
                    PreparedStatement preparedStatement = this.connection.prepareStatement(sqlDropFKQuery);
                    preparedStatement.execute();
                    LOGGER.debug("Dropped constraint");
                } catch (SQLException exception) {
                    LOGGER.error("No constraints found", new ORMException(exception.getMessage()));
                }
            }
        }
    }
}
