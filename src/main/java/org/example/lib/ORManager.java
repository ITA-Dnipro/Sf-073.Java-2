package org.example.lib;

import org.example.lib.configs.*;
import org.example.lib.exception.ExistingObjectException;
import org.example.lib.exception.ORMException;
import org.example.lib.utils.ConnectionUtils;

import javax.sql.DataSource;
import java.io.Serializable;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

public interface ORManager {

    static ORManager withPropertiesFrom(String filename) throws SQLException {
        Properties properties = PropertyConfiguration.readPropertiesFromFile(Path.of(filename));
        return new ORManagerImpl((DataSource) ConnectionUtils.createConnection(properties));
    }

    static ORManager withDataSource(DataSource dataSource) throws SQLException {
        return new ORManagerImpl(HikariCPDataSource.getHikariDatasourceConfiguration(dataSource));
    }

    void register(Class<?>... entityClasses);

    <T> T save(T o) throws SQLException, ORMException;

    void persist(Object o) throws ExistingObjectException, ORMException;

    <T> Optional<T> findById(Serializable id, Class<T> cls) throws ORMException;

    <T> List<T> findAll(Class<T> cls) throws ORMException;

    <T> Iterable<T> findAllAsIterable(Class<T> cls) throws Exception;

    <T> Stream<T> findAllAsStream(Class<T> cls);

    <T> T merge(T o);

    <T> T refresh(T o);

    boolean delete(Object o) throws ORMException;
}
