package org.example.lib;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.db.type.*;
import org.example.configs.PropertyConfiguration;
import org.example.entity.Book;
import org.example.lib.utils.*;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.assertj.db.api.Assertions.assertThat;

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
    void should_Check_If_Entity_Exists_In_Db() {
        Request requestFirstBook = new Request(h2Source, ConstantsQueries.SqlSelectQueries.SELECT_FIRST_BOOK);
        assertThat(requestFirstBook).row(0)
                .hasValues(1, "Solaris", "1961-01-01");
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
        Assertions.assertEquals("TITLE, PUBLISHED_AT", SqlUtils.getColumnNamesInsert("BOOK", orManagerImpl.getConnection()));
        Assertions.assertEquals("ID, TITLE, PUBLISHED_AT", SqlUtils.getColumnNamesUpdate("BOOK", orManagerImpl.getConnection()));
    }

    @Test
    void should_Return_Saved_Entity_From_Db() throws Exception {
        Book book = new Book("Sheherezaza Tales1", LocalDate.of(1956, 3, 24));
        Book bookRecord = orManager.save(book);
        orManager.findAll(Book.class).forEach(System.out::println);
    }
}