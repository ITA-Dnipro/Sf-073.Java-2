package org.example.lib;

import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
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
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS";
    private static final String FIND_ALL = "SELECT * FROM ";
    private static final String FOREIGN_KEY = " FOREIGN KEY (%s) REFERENCES %s(%s)";

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
                ArrayList<String> sql = new ArrayList<>();
                for (Field field : declaredFields) {
                    Class<?> fieldType = field.getType();
                    if (field.isAnnotationPresent(Id.class)) {
                        EntityUtils.setColumnType(sql, fieldType, EntityUtils.getFieldName(field));
                    } else if (field.isAnnotationPresent(Column.class)) {
                        EntityUtils.setColumnType(sql, fieldType, EntityUtils.getFieldName(field));
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
                LOGGER.info(String.format("%s with that ID doesnt exist", cls.getSimpleName()));
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
                        !declaredField.isAnnotationPresent(ManyToOne.class)) {
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
            } else if (field.getType() == field.getType()) {
                Optional<Object> optionalPublisher = findById(value, (Class<Object>) field.getType());
                if (optionalPublisher.isEmpty()) {
                    LOGGER.info(String.format("This %s has no %s record!", entity.getClass().getSimpleName(), field.getType().getSimpleName()));
                }
                optionalPublisher.ifPresent(o -> {
                    try {
                        field.set(entity, o);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IllegalAccessException exception) {
            LOGGER.error(String.format("Unsupported type %s", field.getType()), new UnsupportedTypeException(exception.getMessage()));
        } catch (ORMException e) {
            LOGGER.error("Object with this ID does not exists");
        }
        return entity;
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) throws ORMException {
        List<T> result = new ArrayList<>();
        String sql = FIND_ALL + EntityUtils.getTableName(cls) + ";";
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
    public <T> T refresh(T o) {
        return null;
    }

    @Override
    public boolean delete(Object o) throws ORMException {
        String tableName = EntityUtils.getTableName(o.getClass());

        String deleteQuery = null;
        Field idField;
        String idName;
        Object idValue;
        ResultSet resultSet;

        if (o.getClass().equals(Book.class)) {
            String sql2 = String.format("SELECT `id` FROM %s WHERE `title` = '%s';", tableName, ((Book) o).getTitle());
            try {
                resultSet = this.connection.prepareStatement(sql2).executeQuery();
                while (resultSet.next()) {
                    idField = EntityUtils.getIdColumn(o.getClass());
                    idName = EntityUtils.getSQLColumName(idField);
                    Long id = resultSet.getLong("ID");
                    idField.setAccessible(true);
                    idField.set(o, id);
                    idValue = EntityUtils.getFieldIdValue(o, idField);
                    deleteQuery = String.format("DELETE FROM %s WHERE %s = %s", tableName, idName, idValue);
                    LOGGER.info(String.format("Deleted %s with title %s", o.getClass().getSimpleName(),
                                              ((Book) o).getTitle()));
                }
            } catch (Exception exception) {
                LOGGER.error(String.format("Cannot delete %s with that ID", o.getClass().getSimpleName()),
                             new ORMException(exception.getMessage()));
            }
        } else if (o.getClass().equals(Publisher.class)) {
            String sql2 = String.format("SELECT `id` FROM %s WHERE `name` = '%s';", tableName,
                                        ((Publisher) o).getName());
            try {
                resultSet = this.connection.prepareStatement(sql2).executeQuery();

                while (resultSet.next()) {
                    idField = EntityUtils.getIdColumn(o.getClass());
                    idName = EntityUtils.getSQLColumName(idField);
                    Long id = resultSet.getLong("ID");
                    idField.setAccessible(true);
                    idField.set(o, id);
                    idValue = EntityUtils.getFieldIdValue(o, idField);
                    deleteQuery = String.format("DELETE FROM %s WHERE %s = %s", tableName, idName, idValue);
                    LOGGER.info(String.format("Deleted %s with title %s", o.getClass().getSimpleName(),
                                              ((Publisher) o).getName()));
                }
            } catch (Exception exception) {
                LOGGER.error(String.format("Cannot delete %s with that ID", o.getClass().getSimpleName()),
                             new ORMException(exception.getMessage()));
            }
        }

        PreparedStatement deleteStatement;
        try {
            deleteStatement = connection.prepareStatement(deleteQuery);
            deleteStatement.execute();
            return true;
        } catch (SQLException exception) {
            throw new ORMException(exception.getMessage());
        }
    }
}
