package org.example.lib.configs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.lib.utils.Constants;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Properties;

public class HikariCPDataSource {

    private static final Path path = Path.of(Constants.Connection.PROPERTIES_FILE_NAME);
    private static final Properties properties = PropertyConfiguration.readPropertiesFromFile(path);
    private static final HikariConfig hikariConfig = new HikariConfig();

    private HikariCPDataSource() {}

    public static DataSource getHikariDatasourceConfiguration(DataSource dataSource){
        hikariConfig.setDriverClassName(getDriverClassNameFromConfiguration());
        hikariConfig.setJdbcUrl(String.valueOf(properties.getProperty(Constants.Connection.URL)));
        hikariConfig.setUsername(String.valueOf(properties.getProperty(Constants.Connection.USERNAME)));
        hikariConfig.setPassword(String.valueOf(properties.getProperty(Constants.Connection.PASSWORD)));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource = new HikariDataSource(hikariConfig);
        return dataSource;
    }

    private static String getDriverClassNameFromConfiguration() {
        String connectionDriver = HikariCPDataSource.properties.getProperty(Constants.Connection.DRIVER);
        switch (connectionDriver) {
            case "h2.Driver":
                return Constants.Connection.H2_DRIVER_CLASS_NAME;
            case "mysql.Driver":
                return Constants.Connection.MYSQL_DRIVER_CLASS_NAME;
            case "postgresql.Driver":
                return Constants.Connection.POSTGRES_DRIVER_CLASS_NAME;
            default:
                throw new IllegalArgumentException("no such driver predefined property!");
        }
    }
}
