package org.example.lib;

import org.example.configs.HikariCPDataSource;
import org.example.configs.PropertyConfiguration;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public interface ORManager {

    static ORManager withPropertiesFrom(String filename) {
        Properties properties = PropertyConfiguration.readPropertiesFromFile(Path.of(filename));
        Connection connection = null;
        try (Connection conn = DriverManager
                .getConnection(properties.getProperty("orm.connection.url"),
                        properties.getProperty("orm.connection.username"),
                        properties.getProperty("orm.connection.password"));){
            connection = conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ORManagerImpl(connection);
    }

    static ORManager withDataSource(DataSource dataSource) {
        Connection connection = null;
        try {
            connection = HikariCPDataSource.getHikariDatasourceConfiguration(dataSource).getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ORManagerImpl(connection);
    }
}
