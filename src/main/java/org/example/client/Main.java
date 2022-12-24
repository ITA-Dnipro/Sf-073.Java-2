package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.example.lib.ORManager;
import org.example.lib.ORManagerImpl;

import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException {

        ORManager orManager = ORManager.withDataSource(new HikariDataSource());

        orManager.register(Book.class, Publisher.class);

        Book book2 = new Book("Just Book", LocalDate.now());
        Publisher publisher2 = new Publisher("Just Publisher");

        orManager.persist(book2);
        orManager.persist(publisher2);
    }
}
