package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.example.lib.ORManager;
import org.example.lib.ORManagerImpl;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        ORManagerImpl ormManager = (ORManagerImpl) ORManager.withDataSource(new HikariDataSource());

        ormManager.register(Book.class, Publisher.class);
    }
}
