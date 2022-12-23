package org.example.lib;

import org.example.lib.annotation.Column;
import org.example.lib.annotation.Entity;
import org.example.lib.annotation.Id;
import org.example.lib.annotation.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AnnotationTest {

    @Test
    void test_entityAnnotationIsPresent_when_classIsMarked_should_returnTrue() {

        @Entity
        class WithEntity {
        }

        var res = Utils.entityAnnotationIsPresent(WithEntity.class);

        assertTrue(res);
    }

    @Test
    void test_entityAnnotationIsNotPresent_when_classIsNotMarked_should_returnFalse() {

        class WithoutEntity {
        }

        var res = Utils.entityAnnotationIsPresent(WithoutEntity.class);

        assertFalse(res);
    }

    @Test
    void test_getTableName_when_tableAnnotationIsAbsent_should_takeNameOfTheClass() {

        @Entity
        @Table()
        class WithoutTableAnnotation {
        }

        var res = Utils.getTableName(WithoutTableAnnotation.class);

        assertEquals("WithoutTableAnnotation", res);
    }

    @Test
    void test_getTableName_when_tableAnnotationIsPresent_should_takeNameFromAnnotation() {

        @Entity
        @Table(name = "with_table_annotation")
        class WithTableAnnotation {
        }

        var res = Utils.getTableName(WithTableAnnotation.class);

        assertEquals("with_table_annotation", res);
    }

    @Test
    void rest_getIdField_when_idIsAbsent_should_returnNull() {
        @Entity
        class WithoutIdField {
        }

        var res = Utils.getIdField(WithoutIdField.class);

        assertNull(res);

    }

    @Test
    void rest_getIdField_when_idIsPresent_should_returnNotNull() {
        @Entity
        class WithoutIdField {
            @Id
            Long id;
        }

        var res = Utils.getIdField(WithoutIdField.class);

        assertNotNull(res);
    }

    @Test
    void rest_getIdField_should_returnColumnName() {
        @Entity
        class WithoutIdField {
            @Id
            Long id;
        }
        FieldInfo fieldInfo = Utils.getIdField(WithoutIdField.class);

        String columnName = fieldInfo.getColumnName();

        assertEquals(columnName, "id");
    }

    @Test
    void test_getIdField_when_columnAnnotationIsPresent_should_returnColumnNameBasedOnAnnotation() {
        @Entity
        class WithColumn {
            @Id
            @Column(name = "id_column")
            Long id;
        }

        String expectedColumnName = "id_column";
        FieldInfo fieldInfo = Utils.getIdField(WithColumn.class);
        String columnName = fieldInfo.getColumnName();

        assertEquals(expectedColumnName, columnName);
    }

    @Test
    void test_createTableDdl_when_MinimalEntityIsProvided_should_produceExpectedDDL() {
        @Entity
        class MinimalEntity {
            @Id
            Long id;
        }
        String expectedDdl = """
                create table if not exists MinimalEntity (
                  id bigint not null,
                  primary key (id)
                );
                """;

        String res = Utils.createTableDdl(MinimalEntity.class);
        
        assertThat(res.toLowerCase()).isEqualToIgnoringWhitespace(expectedDdl.toLowerCase());
    }

}
