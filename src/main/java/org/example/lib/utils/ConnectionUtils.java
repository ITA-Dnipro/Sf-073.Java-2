package org.example.lib.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionUtils {

    private ConnectionUtils() {}

    public static Connection createConnection(Properties properties) {
        Connection connection = null;
        try (Connection conn = DriverManager
                .getConnection(properties.getProperty(Constants.Connection.URL),
                        properties.getProperty(Constants.Connection.USERNAME),
                        properties.getProperty(Constants.Connection.PASSWORD))) {
            connection = conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
