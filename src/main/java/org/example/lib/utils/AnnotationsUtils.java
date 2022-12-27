package org.example.lib.utils;

import org.example.lib.annotation.Column;
import org.example.lib.annotation.Entity;
import org.example.lib.annotation.Id;
import org.example.lib.annotation.Table;

import java.lang.reflect.Field;

public class AnnotationsUtils {

    public static boolean entityAnnotationIsPresent(Class<?> cls) {
        return cls.isAnnotationPresent(Entity.class);
    }

    public static String getTableName(Class<?> cls) {
        Table annotation = cls.getAnnotation(Table.class);
        if (annotation != null) {
            String name = annotation.name();
            if (!name.equals("")) {
                return name;
            }
        }
        return cls.getSimpleName();
    }

    public static FieldInfo getIdField(Class<?> cls) {
        FieldInfo res = null;
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                res = new FieldInfo(field);
                break;
            }
        }
        return res;
    }

    public static String createTableDdl(Class<?> cls) {
        String sql = String.format("create table if not exists %s ( %n", cls.getSimpleName());
        if (cls.isAnnotationPresent(Entity.class)) {
            Field[] declaredFields = cls.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Id.class)) {
                    sql += String.format("  id bigint not null,%n  primary key (id)%n);");
                }
            }
        }
        return sql;
    }
}

class FieldInfo {
    String columnName;

    public FieldInfo(Field field) {
        columnName = extractColumnName(field);
    }

    public String getColumnName() {
        return columnName;
    }

    private String extractColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null) {
            String name = columnAnnotation.name();
            if (!name.equals("")) {
                return name;
            }
        }
        return field.getName();
    }
}