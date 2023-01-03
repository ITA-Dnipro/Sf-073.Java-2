package org.example.lib;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.db.type.*;
import org.example.lib.configs.PropertyConfiguration;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.utils.*;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import java.util.Properties;


class MethodSaveTest {
    private static final Path path = Path.of(Constants.Connection.PROPERTIES_FILE_NAME);
    private static final Properties properties = PropertyConfiguration.readPropertiesFromFile(path);
    private final Source h2Source = new Source(properties.getProperty(Constants.Connection.URL), "", "");
    private static ORManager orManager;
    private static ORManagerImpl orManagerImpl;

    static {
        try {
            orManager = ORManager.withDataSource(new HikariDataSource());
            orManagerImpl = (ORManagerImpl) orManager;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    MethodSaveTest() throws SQLException {
    }

    @Test
    void should_Check_If_Method_HasId_Is_Correct() throws Exception {
        Book book = new Book("Hibernate101", LocalDate.of(2000, 10, 5));
        Assertions.assertFalse(EntityUtils.hasId(book));
        List<Book> bookList = orManager.findAll(Book.class);
        Assertions.assertTrue(EntityUtils.hasId(bookList.get(0)));

    }

    @Test
    void should_Retrieve_Column_Names_From_Table() throws Exception {
        Assertions.assertEquals("TITLE, PUBLISHED_AT, PUBLISHER", SqlUtils.getColumnNamesInsert("BOOKS", orManagerImpl.getConnection()));
        Assertions.assertEquals("ID, TITLE, PUBLISHED_AT, PUBLISHER", SqlUtils.getColumnNamesUpdate("BOOKS", orManagerImpl.getConnection()));
        Assertions.assertEquals("NAME", SqlUtils.getColumnNamesInsert("PUBLISHERS", orManagerImpl.getConnection()));
        Assertions.assertEquals("ID, NAME", SqlUtils.getColumnNamesUpdate("PUBLISHERS", orManagerImpl.getConnection()));
    }

    @Test
    void should_Return_Saved_Entity_From_Db() throws Exception {
        Book book = new Book("Story Tales1", LocalDate.of(1998, 3, 24));
        Book bookRecord = orManager.save(book);
        Assertions.assertEquals("Story Tales1", bookRecord.getTitle());
        Assertions.assertEquals(LocalDate.of(1998, 3, 24), bookRecord.getPublishedAt());
    }

    @Test
    void should_Check_If_OneToMany_Object_Is_Present() throws Exception {
        Publisher publisher = new Publisher("New Publisher Test29 @OneToMany");
        Publisher pubFromDb = orManager.save(publisher);
        Book book = new Book("Book With Publisher Test29", LocalDate.of(1998, 3, 24), pubFromDb);
        Book bookWithPublisher = orManager.save(book);
        System.out.println(bookWithPublisher);
        System.out.println(pubFromDb.getBooks());
    }
}
