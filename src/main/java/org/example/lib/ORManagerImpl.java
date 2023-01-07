package org.example.lib;

import org.example.client.entity.Book;
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

import static java.lang.System.*;


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
            try(ResultSet rs = connection.prepareStatement(SqlUtils.findByIdQuery(id, o.getClass())).executeQuery()){
                rs.next();
                EntityUtils.addNewRecordToAssociatedManyToOneCollection(o, rs, associatedManyToOneEntities);
                EntityUtils.saveNewRecordFromAssociatedOneToManyCollection(this, o);
                String successMessage = "Successfully SAVED" + o + "to the database";
                LOGGER.info(successMessage);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
        }
        return o;
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
                T obj = mapObject(cls, resultSet);
                result.add(obj);
            }
        } catch (ORMException | SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchFieldException | ClassNotFoundException exception) {
            LOGGER.error("Something went wrong");
            throw new ORMException(exception.getMessage());
        }
        return result;
    }

    private <T> T mapObject(Class<T> cls, ResultSet resultSet) throws ORMException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, NoSuchFieldException, ClassNotFoundException {
        T obj = cls.getConstructor().newInstance();
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            String fieldName = EntityUtils.getFieldName(field);
            String fieldValue;
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Column.class)) {
                fieldValue = resultSet.getString(fieldName);
                EntityUtils.fillData(obj, field, fieldValue);
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                fieldValue = resultSet.getString(fieldName);
                if (fieldValue != null) {
                    Class<?> type = field.getType();
                    Optional<?> findById = findById(Integer.parseInt(fieldValue), type);
                    Object publisher = findById.get();
                    field.set(obj, publisher);

                    // adds the books to publisher's list
                    Field books = findById.get().getClass().getDeclaredField("books");
                    books.setAccessible(true);
                    Method method = Class.forName("java.util.List").getMethod("add", Object.class);
                    List<T> booksList = new ArrayList<>();
                    String sql2 = "select * from books where " + fieldName + " = " + fieldValue;
                    List<Book> list = new ArrayList<>();
                    try (ResultSet rs = getConnection().prepareStatement(sql2).executeQuery()) {
                        while (rs.next()) {
                            int aLong = rs.getInt(1);
                            Optional<Book> byId1 = findById(aLong, Book.class);
                            Book book = byId1.get();
                            list.add(book);
                        }
                    }
                    method.invoke(booksList, obj);
                    books.set(publisher, list);
                }
            } else if (field.isAnnotationPresent(OneToMany.class)) {
                if (field.getType() == List.class) {
                    field.setAccessible(true);
                    Field id = obj.getClass().getDeclaredField("id");
                    id.setAccessible(true);
                    Object entityId = id.get(obj);
                    String query = "select * from " + fieldName + " where publisher_id = " + entityId;
                    List<Book> list = new ArrayList<>();
                    ResultSet rs = getConnection().prepareStatement(query).executeQuery();
                    while (rs.next()) {
                        int bookId = rs.getInt(1);
                        Optional<Book> byId = findById(bookId, Book.class);
                        Book book = byId.get();
                        list.add(book);
                    }
                    field.set(obj, list);
                }
            }
        }
        return obj;
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
        Map<String, Object> associatedEntities = new HashMap<>();
        T updatedRecord = null;
        if (EntityUtils.hasId(o)) {
            try(ResultSet rs = connection
                    .prepareStatement(SqlUtils.findByIdQuery(o),
                            ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_UPDATABLE).executeQuery()){
                rs.next();
                EntityUtils.updateResultSetExecution(o, rs, rs.getMetaData());
                rs.updateRow();
                Optional<?> optionalRecord = findById(EntityUtils.getId(o), o.getClass());
                updatedRecord = optionalRecord.map(value -> (T) value).orElse(o);;
                EntityUtils.addNewRecordToAssociatedManyToOneCollection(updatedRecord, rs, associatedEntities);
                String successMessage = "Successfully MERGED" + updatedRecord + "to the database";
                LOGGER.info(successMessage);
            }catch(SQLException | IllegalAccessException | ORMException ex){
                LOGGER.error("Error in MERGE method occurred!");
                ex.printStackTrace();
            }
        }
        return updatedRecord;
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
