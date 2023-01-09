package org.example.lib.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;
import org.example.lib.ORManagerImpl;
import org.example.lib.exception.ORMException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import static java.lang.System.*;

class EntityUtilsTest {

    static ORManager orManager;
    static ORManagerImpl orManagerImpl;
    static Connection connection;

    @BeforeAll
    static void setUp() throws SQLException{
        orManager = ORManager.withDataSource(new HikariDataSource());
        orManagerImpl = (ORManagerImpl) orManager;
        connection = orManagerImpl.getConnection();
    }

    @Test
    void should_Check_If_Method_HasId_Is_Correct() throws Exception {
        Book book = new Book("Hibernate101", LocalDate.of(2000, 10, 5));
        Assertions.assertFalse(EntityUtils.hasId(book));
        List<Book> bookList = orManager.findAll(Book.class);
        Assertions.assertTrue(EntityUtils.hasId(bookList.get(0)));

    }


    @Test
    void should_Test_Method_Normalize_SQL_To_Java_Types_With_Values() throws IllegalAccessException, NoSuchFieldException {
        Book book1 = new Book("Test Book1", LocalDate.of(2022, 3, 14));
        Book book2 = new Book("Test Book2", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher1");


    }


    @Test
    void should_Test_Collection_From_collectRecordColumnTypeValues_Method() throws SQLException, ORMException, IllegalAccessException, ClassNotFoundException {
        Publisher publisher = orManager.save(new Publisher("Publisher113"));
        Object bookWithPublisher = orManager.save(new Book("Book113", LocalDate.now(), publisher));
        Map<String, Object> associatedManyToOneEntities = new HashMap<>();

        ResultSet resultSet = connection.prepareStatement(SqlUtils.findByIdQuery(bookWithPublisher)).executeQuery();
        resultSet.next();
        out.println("BookWithPublisher -> values from DB -> saved From bookWithPublisher object");
        Map<String, List<Object>> actualRecordCollection = EntityUtils.collectRecordColumnTypeValues(resultSet);
        actualRecordCollection.forEach((key, value) -> out.println(key + " : " + value));
        out.println("-------------------------------------");
        out.println("-------------------------------------");
        out.println("BookWithPublisher -> values from ENTITY -> saved From bookWithPublisher object");
        Map<String, List<Object>> actualBookWithPubCollection = EntityUtils.collectEntityFieldTypeValues(bookWithPublisher, associatedManyToOneEntities);
        actualBookWithPubCollection.forEach((key, value) -> out.println(key + " : " + value));
        out.println("-------------------------------------");
        out.println("-------------------------------------");
        out.println(publisher.getBooks());
        out.println("-------------------------------------");
        out.println("-------------------------------------");
    }


    }

