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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    void should_Test_Collection_From_collectEntityFieldTypeValues_Method() throws IllegalAccessException {
        Book book = new Book("Test Book3", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher2");
        Map<String, List<Object>> actualCollection = EntityUtils.collectEntityFieldTypeValues(book);
        actualCollection.forEach((key, value) -> System.out.println(key + " : " + value));
    }

    @Test
    void should_Test_Collection_From_collectRecordColumnTypeValues_Method() throws SQLException, ORMException {
        Book book = new Book("Test Book37", LocalDate.of(2021, 2, 24), new Publisher("pub1"));
        Publisher publisher = new Publisher("Publisher37");
        Book bookRecord = orManager.save(book);
        Publisher publisherRecord = orManager.save(publisher);
        ResultSet resultSet = connection.prepareStatement(SqlUtils.findByIdQuery(bookRecord)).executeQuery();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        resultSet.next();

        Map<String, List<Object>> actualCollection = EntityUtils.collectRecordColumnTypeValues(resultSet, resultSetMetaData);
        actualCollection.forEach((key, value) -> System.out.println(key + " : " + value));

        System.out.println();

        Book mergedBook = orManager.merge(bookRecord);
        System.out.println(mergedBook);


    }
    }

