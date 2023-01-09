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
        Publisher publisher3 = orManager.save(new Publisher("Publisher808"));
        Publisher publisher4 = orManager.save(new Publisher("Publisher909"));
        Book bookWithPublisher1 = orManager.save(new Book("Book1P606", LocalDate.now(), publisher1));
        System.out.println("-----------------------------");
        System.out.println(publisher1.getBooks());
        System.out.println("-----------------------------");
        System.out.println(bookWithPublisher1);
        Book bookWithPublisher4 = orManager.save(new Book("Book4P909", LocalDate.now(), publisher4));
        Book bookWithPublisher3 = orManager.save(new Book("Book3P808", LocalDate.now(), publisher3));
        Book bookWithPublisher2 = orManager.save(new Book("Book2P707", LocalDate.now(), publisher2));
        bookWithPublisher1.setPublisher(publisher3);
//        bookWithPublisher4.setPublisher(publisher3);
//        System.out.println("-----------------------------");
//        System.out.println("-----------------------------");
        bookWithPublisher1 = orManager.merge(bookWithPublisher1);
        System.out.println(bookWithPublisher1);
        System.out.println(publisher1.getBooks());
//        bookWithPublisher4 = orManager.merge(bookWithPublisher4);
//        System.out.println(bookWithPublisher4);
//        System.out.println("-----------------------------");
//        bookWithPublisher4 = orManager.merge(bookWithPublisher4);
//        System.out.println(bookWithPublisher4);
//        System.out.println("-----------------------------");
//        System.out.println("-----------------------------");
//        System.out.println("Publisher808 books: " + publisher3.getBooks());
//        System.out.println("Publisher909 books: " + publisher4.getBooks());
//        bookWithPublisher1.setTitle("Book#P606UPDATED");
//        System.out.println(bookWithPublisher1.getTitle());
//        bookWithPublisher2.setTitle("Book#P707UPDATED");
//        System.out.println(bookWithPublisher2.getTitle());
//        System.out.println("-----------------------------");
//        System.out.println("-----------------------------");
//        orManager.merge(bookWithPublisher1);
//        System.out.println(bookWithPublisher1);
//        System.out.println("-----------------------------");
//        orManager.merge(bookWithPublisher2);
//        System.out.println(bookWithPublisher2);
//        System.out.println("-----------------------------");
//        publisher1.setName("Publisher606UPDATED");
//        orManager.merge(publisher3);
//        System.out.println(publisher3.getBooks());



    }
}
