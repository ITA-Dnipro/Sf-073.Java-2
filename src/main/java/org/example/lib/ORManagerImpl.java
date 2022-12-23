package org.example.lib;

import java.sql.Connection;

public class ORManagerImpl implements ORManager{

    private final Connection connection;

    public ORManagerImpl(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return this.connection;
    }
}
