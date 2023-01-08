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
                    LOGGER.error("Cannot create table without annotation @Entity or fields without annotation @Id, @Column or @ManyToOne", new ORMException(exception.getMessage()));
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
            PreparedStatement preparedStatement = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            while (resultSet.next()) {
                EntityUtils.entityIdGenerator(o, resultSet);
            }
            resultSet.close();
            LOGGER.info(String.format("Successfully added %s to the database", o.getClass().getSimpleName()));
        } catch (SQLException exception) {
            LOGGER.error(String.format("%s with that name already exists in the database. The name of the %s should be UNIQUE"
                    , o.getClass().getSimpleName(), o.getClass().getSimpleName()), new ExistingObjectException(exception.getMessage()));
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
                T obj = mapObject(cls, resultSet);
                result.add(obj);
            }
        } catch (SQLException exception) {
            LOGGER.error("Something went wrong");
            throw new ORMException(exception.getMessage());
        }
        return result;
    }
    private <T> T mapObject(Class<T> cls, ResultSet resultSet) {
        T entity = null;
        try {
            entity = cls.getConstructor().newInstance();
            Field[] declaredFields = entity.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                String fieldName = EntityUtils.getFieldName(field);
                String fieldValue;
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Column.class)) {
                    fieldValue = resultSet.getString(fieldName);
                    EntityUtils.fillData(entity, field, fieldValue);
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    fieldValue = resultSet.getString(fieldName);
                    if (fieldValue != null) {
                        Class<?> fieldType = field.getType();
                        Object o = fieldType.getConstructor().newInstance();
                        String publisherTableName = o.getClass().getAnnotation(Table.class).name();
                        String findPublisherQuery = "select * from " + publisherTableName + " where id = " + fieldValue;
                        Object publisher = fieldType.getConstructor().newInstance();
                        String booksField = "";
                        Class<?> booksFieldType = null;
                        for (Field publisherField : publisher.getClass().getDeclaredFields()) {
                            ResultSet rs = getConnection().prepareStatement(findPublisherQuery).executeQuery();
                            while (rs.next()) {
                                String f = EntityUtils.getFieldName(publisherField);
                                if (!publisherField.isAnnotationPresent(OneToMany.class)) {
                                    String v = rs.getString(f);
                                    EntityUtils.fillData(publisher, publisherField, v);
                                } else {
                                    booksField = publisherField.getName();
                                    booksFieldType = publisherField.getType();
                                }
                            }
                        }
                        field.set(entity, publisher);
                        if (booksFieldType == List.class) {
                            addBooksToPublishersList(entity, fieldName, fieldValue, publisher, booksField);
                        }
                    }
                } else if (field.isAnnotationPresent(OneToMany.class)) {
                    if (field.getType() == List.class) {
                        field.setAccessible(true);
                        Field idField = entity.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        Object publisherID = idField.get(entity);
                        String selectPublisherQuery = "select * from " + fieldName + " where " + entity.getClass().getSimpleName() + "_id = " + publisherID;
                        List<Object> publisherBooks = new ArrayList<>();
                        try (ResultSet rs = getConnection().prepareStatement(selectPublisherQuery).executeQuery()) {
                            while (rs.next()) {
                                int bookId = rs.getInt(1);
                                Field booksField = cls.getDeclaredField(fieldName);
                                booksField.setAccessible(true);
                                Object book = getParameterTypeOfTheList(booksField);
                                String findBookQuery = "select * from " + fieldName + " where id = " + bookId;
                                mapBook(book, findBookQuery);
                                publisherBooks.add(book);
                            }
                        }
                        field.set(entity, publisherBooks);
                    }
                }
            }
        } catch (SQLException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchFieldException throwables) {
            throwables.printStackTrace();
        }
        return entity;
    }

    private <T> void addBooksToPublishersList(T entity, String fieldName, String fieldValue, Object publisher, String booksField) throws NoSuchFieldException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLException {
        Field books = publisher.getClass().getDeclaredField(booksField);
        books.setAccessible(true);
        List<Object> booksList = new ArrayList<>();
        String getBook = "select * from " + booksField + " where " + fieldName + " = " + fieldValue;
        Object bookToAdd = entity.getClass().getConstructor().newInstance();
        try (ResultSet rs = getConnection().prepareStatement(getBook).executeQuery()) {
            while (rs.next()) {
                for (Field bookField : bookToAdd.getClass().getDeclaredFields()) {
                    String name = EntityUtils.getFieldName(bookField);
                    String val = rs.getString(name);
                    EntityUtils.fillData(bookToAdd, bookField, val);
                }
                booksList.add(bookToAdd);
                bookToAdd = entity.getClass().getConstructor().newInstance();
            }
        }
        books.set(publisher, booksList);
    }

    private void mapBook(Object book, String findBookQuery) throws SQLException {
        for (Field bookField : book.getClass().getDeclaredFields()) {
            ResultSet rs2 = getConnection().prepareStatement(findBookQuery).executeQuery();
            while (rs2.next()) {
                String f = EntityUtils.getFieldName(bookField);
                String v = rs2.getString(f);
                EntityUtils.fillData(book, bookField, v);
            }
        }
    }

    private Object getParameterTypeOfTheList(Field booksField) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class actualTypeArgument = null;
        if (booksField.getGenericType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) booksField.getGenericType();
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            actualTypeArgument = (Class) actualTypeArguments[0];
        }
        Object book = actualTypeArgument.getConstructor().newInstance();
        return book;
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
                    LOGGER.info(String.format("Deleted %s with title %s", o.getClass().getSimpleName(), ((Book) o).getTitle()));
                }
            } catch (Exception exception) {
                LOGGER.error(String.format("Cannot delete %s with that ID", o.getClass().getSimpleName()),
                        new ORMException(exception.getMessage()));
            }
        } else if (o.getClass().equals(Publisher.class)) {
            String sql2 = String.format("SELECT `id` FROM %s WHERE `name` = '%s';", tableName, ((Publisher) o).getName());
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
                    LOGGER.info(String.format("Deleted %s with title %s", o.getClass().getSimpleName(), ((Publisher) o).getName()));
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
