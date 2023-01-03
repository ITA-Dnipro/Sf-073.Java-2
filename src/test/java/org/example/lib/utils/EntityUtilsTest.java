package org.example.lib.utils;

import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class EntityUtilsTest {

    @Test
    void should_Test_Method_Normalize_SQL_To_Java_Types_With_Values() throws IllegalAccessException, NoSuchFieldException {
        Book book1 = new Book("Test Book1", LocalDate.of(2022, 3, 14));
        Book book2 = new Book("Test Book2", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher1");


    }
    }

