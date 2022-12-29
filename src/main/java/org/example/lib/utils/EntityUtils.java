package org.example.lib.utils;

import org.example.lib.annotation.*;

import java.lang.reflect.*;

import java.util.*;
import java.util.stream.Collectors;


public class EntityUtils {

    private EntityUtils() {}

    public static String getTableName(Class<?> cls) {
        if (cls.isAnnotationPresent(Table.class)) {
            String name = cls.getAnnotation(Table.class).name();
            if (!name.equals("")) {
                return name;
            }
        }
        return cls.getSimpleName();
    }

    public static  String getFieldName(Field field) {
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
            if(declaredField.isAnnotationPresent(ManyToOne.class)){
                declaredField.setAccessible(true);
                Object value = declaredField.get(o);
                result.add(value);
                result.add(",");
            }
        }
        result.remove(result.size()-1);
        return result;
    }

    public static <T> boolean hasId(T o) {
        boolean isEntity = o.getClass().isAnnotationPresent(Entity.class);
        Long id = null;
        try {
            if(isEntity) {
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
            if(isEntity) {
                Method getId = o.getClass().getMethod("getId");
                id = (Long) getId.invoke(o);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }
}
