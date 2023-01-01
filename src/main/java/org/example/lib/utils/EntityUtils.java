package org.example.lib.utils;

import org.example.lib.annotation.*;

import java.lang.reflect.*;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


public class EntityUtils {

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
        }
        return field.getName();
    }

    public static String getFieldValues(Object o) throws IllegalAccessException {
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

    public static List<Object> getFieldValuesWithManyToOne(Object o) throws IllegalAccessException {
        Field[] declaredFields = o.getClass().getDeclaredFields();

        List<Object> result = new ArrayList<>();

        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(Column.class) != null) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(o);
                result.add("'" + value.toString() + "'");
                result.add(",");
            }
            if (declaredField.isAnnotationPresent(ManyToOne.class)) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(o);
                result.add(value);
                result.add(",");
            }
        }
        result.remove(result.size() - 1);
        return result;
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

    public static boolean hasIdAndColumnAnnotation(Field field) {
        return field.isAnnotationPresent(Column.class) ||
                field.isAnnotationPresent(Id.class);
    }

    public static boolean hasManyToOneAnnotation(Field field) {
        return field.isAnnotationPresent(ManyToOne.class);
    }

    public static <T> PreparedStatement setterPreparedStatementExecution(PreparedStatement statement, ResultSetMetaData resultSetMetaData,T o) throws SQLException, IllegalAccessException {
        int columnCount = resultSetMetaData.getColumnCount();
        Field[] fields = o.getClass().getDeclaredFields();
        Map<String, List<Object>> collectFieldTypeValues = new HashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            for (Field field : fields) {
                field.setAccessible(true);
                if (hasIdAndColumnAnnotation(field)) {
                    String[] fieldRawType = field.getType().toString().split(" ");
                    String fieldTypePretty = fieldRawType[1];
                    String columnTypeClassName = resultSetMetaData.getColumnClassName(i);
                    EntityUtils.normalizeSqlToJavaTypesWithValues(collectFieldTypeValues, columnTypeClassName, fieldTypePretty, field, o);
                }
                if(hasManyToOneAnnotation(field)){
                    if(field.get(o) == null){
                        break;
                    }
                    String fieldTypePretty = "entity";
                    String columnTypeClassName = resultSetMetaData.getColumnClassName(i);
                    EntityUtils.normalizeSqlToJavaTypesWithValues(collectFieldTypeValues, columnTypeClassName, fieldTypePretty, field, o);
                }
            }
        }
        for (int i = 1; i+1 <= columnCount; i++) {
            String columnName = resultSetMetaData.getColumnName(i+1).toLowerCase();
            if (collectFieldTypeValues.containsKey(columnName)){
                statement.setObject(i, collectFieldTypeValues.get(columnName).get(1));
                continue;
            }
            statement.setObject(i, null);
        }
        return statement;
    }

    private static Map<String, List<Object>> normalizeSqlToJavaTypesWithValues(Map<String, List<Object>> collectFieldTypeValues, String columnTypeClassName, String fieldRawType, Field field, Object o) throws IllegalAccessException {

        if(columnTypeClassName.equals(fieldRawType) && columnTypeClassName.equals("java.lang.String")){
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.String", field.get(o))));
        }
        if(columnTypeClassName.equals("java.sql.Date") && fieldRawType.equals("java.time.LocalDate")){
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.sql.Date", field.get(o))));
        }
        if(columnTypeClassName.equals(fieldRawType) && columnTypeClassName.equals("java.lang.Long")){
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.Long", field.get(o))));
        }
        if(columnTypeClassName.equals("java.lang.Long") && fieldRawType.equals("entity")){
            Object associatedObject = field.get(o);
            Long id = EntityUtils.getId(associatedObject);
            collectFieldTypeValues.put(EntityUtils.getFieldName(field), new ArrayList<>(Arrays.asList("java.lang.Long", id)));
        }
        return collectFieldTypeValues;
    }
}

