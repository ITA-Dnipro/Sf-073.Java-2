package org.example.lib;

import org.example.configs.HikariCPDataSource;
import org.example.configs.PropertyConfiguration;
import org.example.lib.utils.ConnectionUtils;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public interface ORManager {

    static ORManager withPropertiesFrom(String filename) {
        Properties properties = PropertyConfiguration.readPropertiesFromFile(Path.of(filename));
        return new ORManagerImpl(ConnectionUtils.createConnection(properties));
    }


    static ORManager withDataSource(DataSource dataSource) throws SQLException {
        return new ORManagerImpl(HikariCPDataSource.getHikariDatasourceConfiguration(dataSource).getConnection());
    }

    // generate the schema in the DB
    // for given list of org.example.entity classes (and all related
    //  by OneToMany/ManyToOne) create a schema in DB
    void register(Class<?>... entityClasses) throws Exception;

    // CREATE
    // save a new object to DB, set id if autogenerated
    // or merge into DB if id is present
    <T> T save(T o) throws SQLException;

    // save a new object to DB, set id if autogenerated
    // throw if the object has id already set (except for String)
    void persist(Object o) throws SQLException, IllegalAccessException;

    // READ
    <T> Optional<T> findById(Serializable id, Class<T> cls) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    // READ ALL
    <T> List<T> findAll(Class<T> cls) throws Exception;

    // READ ALL LAZY
    <T> Iterable<T> findAllAsIterable(Class<T> cls) throws Exception; // (MEDIUM)

    <T> Stream<T> findAllAsStream(Class<T> cls);     // (OPTIONAL)

    // UPDATE
    <T> T merge(T o);   // send o -> DB row (to table)

    <T> T refresh(T o); // send o <- DB row (from table)

    // DELETE
    // set autogenerated id to null
    // return true if successfully deleted
    boolean delete(Object o);
}
