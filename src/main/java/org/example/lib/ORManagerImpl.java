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

import static java.lang.System.out;


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
                 PreparedStatement stBefore = connection.prepareStatement(SqlUtils.selectFirstFromTable(o.getClass()))) {
                ResultSetMetaData resultSetMetaData = stBefore.getMetaData();
                EntityUtils.setterPreparedStatementExecution(statement, resultSetMetaData, o, associatedManyToOneEntities);
                statement.executeUpdate();
                ResultSet keys = statement.getGeneratedKeys();
                keys.next();
                id = keys.getLong(EntityUtils.getIdFieldName(o.getClass()));
                EntityUtils.setFieldId(id, o);
            } catch (SQLException | IllegalAccessException | ClassNotFoundException ex) {
                if (ex.getClass().getTypeName().equals("org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException")) {
                    LOGGER.error(String.format("SQL exception: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException saving %s", o.getClass()));
                    LOGGER.info("Possible reason: columns with key constraints NON NULL || UNIQUE prevent saving this record");
                    throw new ExistingObjectException("Please provide non existing entity or check for duplicate constraint fields");
                }
            }
            try (ResultSet rs = connection.prepareStatement(SqlUtils.findByIdQuery(id, o.getClass())).executeQuery()) {
                rs.next();
                EntityUtils.addNewRecordToAssociatedManyToOneCollection(o, rs, associatedManyToOneEntities);
                EntityUtils.saveNewRecordFromAssociatedOneToManyCollection(this, o);
                String successMessage = "Successfully SAVED " + o + " to the database";
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
        return EntityUtils.createEntity(cls, resultSet);
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
        Map<String, Object> associatedManyToOneEntities = new HashMap<>();
        Map<String, Object> associatedOneToManyEntities = new HashMap<>();
        Long id = null;
        if (EntityUtils.hasId(o)) {
            try(ResultSet rs = connection
                    .prepareStatement(SqlUtils.findByIdQuery(o),
                            ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_UPDATABLE).executeQuery()){
                rs.next();
                EntityUtils.updateResultSetExecution(o, rs, associatedManyToOneEntities, associatedOneToManyEntities);
                id = rs.getLong(EntityUtils.getIdFieldName(o.getClass()));
                rs.updateRow();
            }catch(SQLException | IllegalAccessException | ClassNotFoundException ex){
                LOGGER.error("Error in MERGE method occurred!");
                ex.printStackTrace();
            }
            associatedManyToOneEntities.forEach((key, value) -> out.println(key + " : " + value));
            try (ResultSet rs = connection.prepareStatement(SqlUtils.findByIdQuery(id, o.getClass())).executeQuery()) {
                rs.next();
                EntityUtils.addNewRecordToAssociatedManyToOneCollection(o, rs, associatedManyToOneEntities);
                EntityUtils.saveNewRecordFromAssociatedOneToManyCollection(this, o);
                String successMessage = "Successfully MERGED " + o + " to the database";
                LOGGER.info(successMessage);
            } catch (IllegalAccessException | SQLException | ORMException e) {
                e.printStackTrace();
            }
        }
        return o;
    }

    @Override
    public <T> T refresh(T o) {
        return null;
    }

    @Override
    public boolean delete(Object o) throws ORMException {
        String tableName = EntityUtils.getTableName(o.getClass());
        Field idField = EntityUtils.getIdColumn(o.getClass());
        String idName = EntityUtils.getSQLColumName(idField);
        Object idValue = EntityUtils.getFieldIdValue(o, idField);
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
}
