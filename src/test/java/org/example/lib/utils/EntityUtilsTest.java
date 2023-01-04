package org.example.lib.utils;

import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

class EntityUtilsTest {

    @Test
    void should_Test_Method_Normalize_SQL_To_Java_Types_With_Values() throws IllegalAccessException, NoSuchFieldException {
        Book book1 = new Book("Test Book1", LocalDate.of(2022, 3, 14));
        Book book2 = new Book("Test Book2", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher1");


    }
    @Test
    void should_Test_Collection_From_collectEntityFieldTypeValues() throws IllegalAccessException {
        Book book = new Book("Test Book2", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher1");
        Map<String, List<Object>> actualCollection = EntityUtils.collectEntityFieldTypeValues(book);
        actualCollection.forEach((key, value) -> System.out.println(key + " : " + value));
    }
    }

