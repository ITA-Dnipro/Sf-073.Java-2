package org.example.lib.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;
import org.example.lib.ORManagerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class SqlUtilsTest {

    private static Connection connection;

    static {
        try {
            ORManager orManager = ORManager.withDataSource(new HikariDataSource());
            ORManagerImpl orManagerImpl = (ORManagerImpl) orManager;
            connection = orManagerImpl.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void should_Test_String_From_SaveQuery_Method() throws SQLException {
        Book book = new Book("Test Book 1", LocalDate.of(2020, 4, 16));
        Publisher publisher = new Publisher("Publisher Test 1");

        Assertions.assertEquals("INSERT INTO books (TITLE, PUBLISHED_AT, PUBLISHER) VALUES ( ?, ?, ?)",
                SqlUtils.saveQuery(book, Book.class, connection));
        Assertions.assertEquals("INSERT INTO publishers (NAME) VALUES ( ?)",
                SqlUtils.saveQuery(publisher, Publisher.class, connection));

    }
}
