package org.example.lib;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.exception.ORMException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

class MethodMergeTest {

    static ORManager orManager;
    static ORManagerImpl orManagerImpl;
    static Connection connection;

    @BeforeAll
    static void setUp() throws SQLException {
        orManager = ORManager.withDataSource(new HikariDataSource());
        orManagerImpl = (ORManagerImpl) orManager;
        connection = orManagerImpl.getConnection();
        orManager.register(Publisher.class);
        orManager.register(Book.class);

    }

    @BeforeEach
    void deleteAllRecords() throws SQLException {
        deleteAllRowsFromTable("books");
        deleteAllRowsFromTable("publishers");
    }

    public static void deleteAllRowsFromTable(String tableName) throws SQLException {
        connection.prepareStatement("DELETE FROM " + tableName).execute();
    }

    @Test
    void should_Test_Merge_Method() throws SQLException, ORMException {
        Publisher publisher1 = orManager.save(new Publisher("Publisher606"));
        Publisher publisher2 = orManager.save(new Publisher("Publisher707"));
        Book bookWithPublisher1 = orManager.save(new Book("Book1P606", LocalDate.now(), publisher1));
        Book bookWithPublisher4 = orManager.save(new Book("Book4P909", LocalDate.now(), publisher1));
        Book bookWithPublisher3 = orManager.save(new Book("Book3P808", LocalDate.now(), publisher1));

        bookWithPublisher1.setPublisher(publisher2);
        orManager.merge(bookWithPublisher1);

        System.out.println(publisher2.getBooks());
        System.out.println(publisher1.getBooks());




    }
}
