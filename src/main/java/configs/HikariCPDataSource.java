package configs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Properties;

public class HikariCPDataSource {

    private static final Path path = Path.of("orm.properties");
    private static final Properties properties = PropertyConfiguration.readPropertiesFromFile(path);
    private static final HikariConfig hikariConfig = new HikariConfig();

    private HikariCPDataSource() {}

    public static DataSource getHikariDatasourceConfiguration(DataSource dataSource){
        hikariConfig.setDriverClassName(getDriverClassNameFromConfiguration());
        hikariConfig.setJdbcUrl(String.valueOf(properties.getProperty("orm.connection.url")));
        hikariConfig.setUsername(String.valueOf(properties.getProperty("orm.connection.username")));
        hikariConfig.setPassword(String.valueOf(properties.getProperty("orm.connection.password")));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource = new HikariDataSource(hikariConfig);
        return dataSource;
    }

    private static String getDriverClassNameFromConfiguration() {
        String connectionDriver = HikariCPDataSource.properties.getProperty("orm.connection.driver");
        switch (connectionDriver) {
            case "h2.Driver":
                return "org.h2.Driver";
            case "mysql.Driver":
                return "com.mysql.jdbc.Driver";
            case "postgresql.Driver":
                return "org.postgresql.Driver";
            default:
                throw new IllegalArgumentException("no such driver predefined property!");
        }
    }
}
