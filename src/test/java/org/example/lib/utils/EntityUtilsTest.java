package org.example.lib.utils;

import org.example.entity.Book;
import org.example.entity.Publisher;
import org.example.lib.annotation.Column;
import org.example.lib.annotation.Id;
import org.example.lib.annotation.ManyToOne;
import org.example.lib.annotation.OneToMany;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

class EntityUtilsTest {


    @Test
    void should_Test_Method_getFieldValuesWithManyToOne_For_Correct_Types_Of_Objects() throws IllegalAccessException {
        Book book1 = new Book("Test Book", LocalDate.of(2020, 3, 14));
        List<Object> objectListFieldValues = EntityUtils.getFieldValuesWithManyToOne(book1);
        objectListFieldValues.forEach(System.out::println);

    }

    @Test
    void should_Test_Method_Normalize_SQL_To_Java_Types_With_Values() throws IllegalAccessException, NoSuchFieldException {
        Book book1 = new Book("Test Book1", LocalDate.of(2022, 3, 14));
        Book book2 = new Book("Test Book2", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher1");


    }
    }

