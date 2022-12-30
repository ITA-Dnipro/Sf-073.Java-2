package org.example.lib.utils;

import org.example.entity.Book;
import org.example.entity.Publisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class EntityUtilsTest {


    @Test
    void should_Test_Method_getFieldValuesWithManyToOne_For_Correct_Types_Of_Objects() throws IllegalAccessException {
        Book book1 = new Book("Test Book", LocalDate.of(2020, 3, 14));
        List<Object> objectListFieldValues = EntityUtils.getFieldValuesWithManyToOne(book1);

        objectListFieldValues.forEach(System.out::println);

    }

    @Test
    void should_Test_Method_CollectEntityFieldData(){
        Book book1 = new Book("Test Book1", LocalDate.of(2022, 3, 14));
        Book book2 = new Book("Test Book2", LocalDate.of(2021, 2, 24));
        Publisher publisher = new Publisher("Publisher1");


    }
}
