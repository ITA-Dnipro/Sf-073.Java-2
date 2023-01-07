package org.example.lib.utils;

import org.example.lib.ORManagerImpl;
import org.example.lib.annotation.*;
import org.example.lib.exception.*;
import org.slf4j.*;

import java.lang.reflect.*;

import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class EntityUtils {

    private static final String ID = " BIGINT PRIMARY KEY AUTO_INCREMENT";
    private static final String NAME = " VARCHAR(255) UNIQUE NOT NULL";
    private static final String DATE = " DATE NOT NULL";
    private static final String LONG = " BIGINT NOT NULL";

    private final static Logger LOGGER = LoggerFactory.getLogger(EntityUtils.class);

    private EntityUtils() {
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

    public static String getFieldName(Field field) {
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

    public static String getFieldValues(Object o) throws ORMException {
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
            LOGGER.debug("Cannot get the values from the entity");
            throw new ORMException("Cannot get the values from the entity");
        }

        return String.join(",", result);
    }

    public static void setColumnType(ArrayList<String> sql, Class<?> type, String name) {
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

    public static String getFieldsWithoutId(Object o) {
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

    public static <T> T fillData(T entity, Field field, String value) {
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
            LOGGER.error(String.format("Unsupported type %s", field.getType()), new UnsupportedTypeException(exception.getMessage()));
        }
        return entity;
    }

    public static <T> Optional<T> createEntity(Class<T> cls, ResultSet resultSet) throws ORMException {
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
                entity = EntityUtils.fillData(entity, declaredField, value);
            }
        } catch (NoSuchMethodException | IllegalAccessException | SQLException | InvocationTargetException |
                 InstantiationException exception) {
            LOGGER.error("Cannot create entity");
            throw new ORMException(exception.getMessage());
        }
        return Optional.of(entity);
    }

    public static <T> boolean hasId(T o) {
        boolean isEntity = o.getClass().isAnnotationPresent(Entity.class);
        Long id = null;
        try {
            if (isEntity) {
                Method getId = o.getClass().getMethod("getId");
                id = (Long) getId.invoke(o);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return id != null;
    }

    public static <T> Long getId(T o) {
        boolean isEntity = o.getClass().isAnnotationPresent(Entity.class);
        Long id = null;
        try {
            if (isEntity) {
                Method getId = o.getClass().getMethod("getId");
                id = (Long) getId.invoke(o);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }

    public static <T> void setFieldId(Long id, T o) {
        boolean isEntity = o.getClass().isAnnotationPresent(Entity.class);
        try {
            if (isEntity) {
                Field[] fields = o.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        field.set(o, id);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static String getIdType(Class<?> cls) {
        Field[] fields = cls.getDeclaredFields();
        String fieldType = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldType = field.getType().getName();
            }
        }
        return fieldType;
    }

    public static String getTableNameFromGenericTypeOneToMany(Field field) throws ClassNotFoundException {
        String fieldGenericType = field.getGenericType().getTypeName();
        String[] strArr = fieldGenericType.split("<");
        String className = (strArr[1].substring(0, strArr[1].length() - 1));
        Class<?> entityClass = Class.forName(className);
        return getTableName(entityClass);
    }

    public static Class<?> getClassNameFromGenericTypeOneToMany(Field field) throws ClassNotFoundException {
        String fieldGenericType = field.getGenericType().getTypeName();
        String[] strArr = fieldGenericType.split("<");
        String className = (strArr[1].substring(0, strArr[1].length() - 1));
        return Class.forName(className);
    }

    public static boolean hasIdAndColumnAnnotation(Field field) {
        return field.isAnnotationPresent(Column.class) ||
                field.isAnnotationPresent(Id.class);
    }

    public static boolean hasManyToOneAnnotation(Field field) {
        return field.isAnnotationPresent(ManyToOne.class);
    }

    public static boolean hasOneToManyAnnotation(Field field) {
        return field.isAnnotationPresent(OneToMany.class);
    }

    public static String getOneToManyMappedByValue(Class<?> cls) {
        Field[] fields = cls.getDeclaredFields();
        String result = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                String mappedBy = field.getAnnotation(OneToMany.class).mappedBy();
                if (!mappedBy.equals("")) {
                    result = mappedBy;
                } else {
                    result = field.getName();
                }
            }
        }
        return result;
    }

    public static <T> List<?> getOneToManyCollection(T o) throws IllegalAccessException {
        Field[] fields = o.getClass().getDeclaredFields();
        List<?> list = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class)) {
               if(field.get(o) == null){
                   return list;
               }
               else{
                   list = (List<?>) field.get(o);
               }
            }
        }
        return list;
    }

    public static String getManyToOneColumnNameValue(Class<?> cls) {
        Field[] fields = cls.getDeclaredFields();
        String result = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                String columnName = field.getAnnotation(ManyToOne.class).columnName();
                if (!columnName.equals("")) {
                    result = columnName;
                } else {
                    result = field.getName();
                }
            }
        }
        return result;
    }

    public static <T> PreparedStatement setterPreparedStatementExecution(PreparedStatement statement,
                                                                         ResultSetMetaData resultSetMetaData,
                                                                         T o,
                                                                         Map<String, Object> associatedManyToOneEntities) throws SQLException, IllegalAccessException, ClassNotFoundException {
        int columnCount = resultSetMetaData.getColumnCount();
        Field[] fields = o.getClass().getDeclaredFields();
        Map<String, List<Object>> collectFieldTypeValues = new HashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            for (Field field : fields) {
                field.setAccessible(true);
                if (hasIdAndColumnAnnotation(field)) {
                    String fieldType = field.getType().getName();
                    String columnTypeClassName = resultSetMetaData.getColumnClassName(i);
                    EntityUtils.normalizeSqlToJavaTypesWithValues(collectFieldTypeValues, columnTypeClassName, fieldType, field, o);
                }
                if (hasManyToOneAnnotation(field)) {
                    if (field.get(o) == null) {
                        break;
                    }
                    if (field.get(o).getClass().isAnnotationPresent(Entity.class)) {
                        associatedManyToOneEntities.put(getManyToOneColumnNameValue(o.getClass()), field.get(o));
                        String fieldTypePretty = "entity";
                        String columnTypeClassName = resultSetMetaData.getColumnClassName(i);
                        EntityUtils.normalizeSqlToJavaTypesWithValues(collectFieldTypeValues, columnTypeClassName, fieldTypePretty, field, o);
                    }
                }
            }
        }
        for (int i = 1; i + 1 <= columnCount; i++) {
            String columnName = resultSetMetaData.getColumnName(i + 1).toLowerCase();
            if (collectFieldTypeValues.containsKey(columnName)) {
                statement.setObject(i, collectFieldTypeValues.get(columnName).get(1));
                continue;
            }
            statement.setObject(i, null);
        }
        return statement;
    }

    private static Map<String, List<Object>> normalizeSqlToJavaTypesWithValues(Map<String, List<Object>> collectFieldTypeValues, String columnTypeClassName, String fieldRawType, Field field, Object o) throws IllegalAccessException {

        if (columnTypeClassName.equals(fieldRawType) && columnTypeClassName.equals("java.lang.String")) {
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.String", field.get(o))));
        }
        if (columnTypeClassName.equals("java.sql.Date") && fieldRawType.equals("java.time.LocalDate")) {
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.sql.Date", field.get(o))));
        }
        if (columnTypeClassName.equals(fieldRawType) && columnTypeClassName.equals("java.lang.Long")) {
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.Long", field.get(o))));
        }
        if (columnTypeClassName.equals("java.lang.Long") && fieldRawType.equals("entity")) {
            Object associatedObject = field.get(o);
            Long id = EntityUtils.getId(associatedObject);
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.Long", id)));
        }
        return collectFieldTypeValues;
    }

    public static <T> void addNewRecordToAssociatedManyToOneCollection(T newRecord, ResultSet resultSet, Map<String, Object> associatedEntities) throws SQLException, IllegalAccessException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (associatedEntities.containsKey(resultSetMetaData.getColumnName(i).toLowerCase())) {
                Object associateEntity = associatedEntities.get(resultSetMetaData.getColumnName(i).toLowerCase());
                Field[] fields = associateEntity.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (EntityUtils.hasOneToManyAnnotation(field)) {
                        if (Objects.equals(field.getType().getName(), "java.util.List") && field.get(associateEntity) != null) {
                            List<T> fieldValue = (List<T>) field.get(associateEntity);
                            List<T> newListValue = new ArrayList<>(fieldValue);
                            newListValue.add(newRecord);
                            field.set(associateEntity, newListValue);
                        }
                    }
                }
            }
        }
    }

    public static String getIdFieldName(Class<?> cls) {
        return "id";
    }

    public static <T> Map<String, List<Object>> collectEntityFieldTypeValues(T object) throws IllegalAccessException, ClassNotFoundException {
        Map<String, List<Object>> entityFieldTypeValues = new HashMap<>();
        Field[] entityFields = object.getClass().getDeclaredFields();
        for (Field field : entityFields) {
            field.setAccessible(true);
            String fieldType = field.getType().getSimpleName().toLowerCase();
            Object fieldValue = field.get(object);
            if (fieldValue == null) {
                entityFieldTypeValues.put(getFieldName(field), new ArrayList<>(Arrays.asList(fieldType, null)));
                continue;
            }
            if (hasIdAndColumnAnnotation(field)) {
                entityFieldTypeValues.put(getFieldName(field), new ArrayList<>(Arrays.asList(fieldType, fieldValue)));
            }
            if (hasManyToOneAnnotation(field)) {
                Class<?> fieldClass = fieldValue.getClass();
                entityFieldTypeValues.put(getOneToManyMappedByValue(fieldClass), new ArrayList<>(Arrays.asList(getIdType(fieldClass), fieldValue)));
            }
            if (hasOneToManyAnnotation(field)) {
                entityFieldTypeValues.put(getTableNameFromGenericTypeOneToMany(field), new ArrayList<>(Arrays.asList(fieldType, fieldValue)));
            }
        }
        return entityFieldTypeValues;
    }

    public static <T> Map<String, List<Object>> collectRecordColumnTypeValues(ResultSet rs) throws SQLException {
        ResultSetMetaData rsMetaData = rs.getMetaData();
        Map<String, List<Object>> recordColumnTypeValues = new HashMap<>();
        int columnCount = rsMetaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            recordColumnTypeValues.put(rsMetaData.getColumnName(i).toLowerCase(), new ArrayList<>(Arrays.asList(rsMetaData.getColumnClassName(i), rs.getObject(i))));
        }
        return recordColumnTypeValues;
    }

    public static <T> ResultSet updateResultSetExecution(T o, ResultSet rs, ResultSetMetaData rsMetaData) throws IllegalAccessException {



        return rs;
    }

    public static <T> void saveNewRecordFromAssociatedOneToManyCollection(ORManagerImpl orManager, T o) throws IllegalAccessException, SQLException, ORMException {
        List<?> list = getOneToManyCollection(o);
        List newList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            newList.add(orManager.save(list.get(i)));
        }
        setOneToManyCollection(o, newList);

    }

    private static void setOneToManyCollection(Object o, List newList) throws IllegalAccessException {
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class)) {
               field.set(o, newList);
            }
        }
    }
    public static Field getIdColumn(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Entity is missing an Id column"));
    }

    public static String getSQLColumName(Field idField) {
        Id idAnnotation = idField.getAnnotation(Id.class);
        String fieldName = null;

        if (idAnnotation != null) {
            fieldName = idField.getName();
        }
        return fieldName;
    }

    public static Object getFieldIdValue(Object o, Field idField) throws ORMException {
        idField.setAccessible(true);
        try {
            return idField.get(o);
        } catch (IllegalAccessException exception) {
            throw new ORMException(exception.getMessage());
        }
    }

    public static <T> void entityIdGenerator(T o, ResultSet generatedKey) {
        Field[] declaredFields = o.getClass().getDeclaredFields();
        try {
            declaredFields[0].setAccessible(true);
            String fieldTypeSimpleName = declaredFields[0].getType().getSimpleName();
            if (fieldTypeSimpleName.equals("Long")) {
                declaredFields[0].set(o, generatedKey.getLong(1));
            }
        } catch (IllegalAccessException | SQLException exception) {
            LOGGER.error(exception.getMessage());
        }
    }
}

