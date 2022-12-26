package org.example.lib.utils;

public final class Constants {

    private Constants() {}

public static final class Connection{

    private Connection() {}

    public static final String PROPERTIES_FILE_NAME = "orm.properties";
    public static final String URL = "orm.connection.url";
    public static final String USERNAME = "orm.connection.username";
    public static final String PASSWORD = "orm.connection.password";
    public static final String DRIVER = "orm.connection.driver";
    public static final String DIALECT = "orm.dialect";

    public static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";
    public static final String MYSQL_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    public static final String POSTGRES_DRIVER_CLASS_NAME = "org.postgresql.Driver";

    }
}
