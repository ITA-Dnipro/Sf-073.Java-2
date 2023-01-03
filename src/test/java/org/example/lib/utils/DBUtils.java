package org.example.lib.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;
import org.example.lib.exception.ORMException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;

public class DBUtils {
    private static DataSource dataSource;
    private static ORManager orManager;

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static ORManager getOrManager() {
        return orManager;
    }

    public static ORManager init() throws ORMException, SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:./src/test/db/test-db");
        dataSource = new HikariDataSource(config);
        orManager = ORManager.withDataSource(dataSource);
        orManager.register(Publisher.class);
        orManager.register(Book.class);
        Book book = new Book("Test Book", LocalDate.now());
        Publisher publisher = new Publisher("Test Publisher");
        orManager.persist(book);
        orManager.persist(publisher);
        return orManager;
    }

    public static void clear() throws SQLException {
        dataSource.getConnection().prepareStatement("drop all objects").execute();

    }
}
