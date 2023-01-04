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
    public void register(Class<?>... entityClasses) throws ORMException {
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
                    LOGGER.error("Cannot create table without annotation @Entity or fields without annotation @Id, @Column or @ManyToOne");
                    throw new ORMException("An error has occurred");
                }
            }
        }
    }

    @Override
    public <T> T save(T o) throws ORMException {
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
                    LOGGER.info(String.format("Successfully added %s to the database", newRecord));
                }
            }catch(SQLException | IllegalAccessException ex){
                if(ex.getClass().getTypeName().equals("org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException")){
                    LOGGER.error(String.format("SQL exception: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException saving %s", o.getClass()));
                    LOGGER.info("Possible reason: columns with key constraints NON NULL || UNIQUE prevent saving this record");
                    LOGGER.info("Possible solution: provide non existing entity or check for duplicate constraint fields");
                }
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
        String tableName = EntityUtils.getTableName(o.getClass());
        String fieldList = EntityUtils.getFieldsWithoutId(o);
        String valueList = EntityUtils.getFieldValues(o);
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);

        try {
            this.connection.prepareStatement(sql).execute();
            LOGGER.info(String.format("Successfully added %s to the database", o.getClass()));
        } catch (SQLException exception) {
            LOGGER.error(String.format("%s with that name already exists in the database. The name of the %s should be UNIQUE", o.getClass(), o.getClass()));
            throw new ExistingObjectException("An error has occurred");
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
                    String name = EntityUtils.getFieldName(field);
                    String value = resultSet.getString(name);
                    field.setAccessible(true);
                    EntityUtils.fillData(obj, field, value);
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
