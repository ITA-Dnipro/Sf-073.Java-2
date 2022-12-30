package org.example.lib;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.db.type.Request;
import org.assertj.db.type.Table;
import org.example.configs.HikariCPDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.db.api.Assertions.assertThat;

public class ORManagerImplTest {
    private static DataSource dataSource;
    private static ORManager orManager;

    @BeforeAll
    static void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:./src/test/db/h2mem-test");
        dataSource = new HikariDataSource(config);
        orManager = ORManager.withDataSource(dataSource);

        orManager.register(Publisher.class);
        orManager.register(Book.class);
    }

    @AfterAll
    static void dropDB() {
       new Request(dataSource, "drop schema h2mem-test");
    }

    @Test
    @DisplayName("Non existing table")
    void test_when_tableDoesNotExist_should_return_tableDoesNotExist() {
        Table table = new Table(dataSource, "tests");

        assertThat(table).doesNotExist();
    }

    @Test
    @DisplayName("Returns all Books Columns correctly")
    void test_when_tableBookExist_should_return_membersCorrectly() {
        Table table = new Table(dataSource, "books");

        assertThat(table).column(0).hasColumnName("id")
                         .column(1).hasColumnName("title")
                         .column(2).hasColumnName("published_at")
                         .column(3).hasColumnName("publisher_id");
    }

    @Test
    @DisplayName("Returns all Publishers Columns correctly")
    void test_when_tablePublisherExist_should_return_membersCorrectly() {
        Table table = new Table(dataSource, "publishers");

        assertThat(table).column(0).hasColumnName("id")
                         .column(1).hasColumnName("name");
    }

    @Test
    @DisplayName("Persist Publisher")
    void test_persist_Publisher_should_return_firstRowValuesCorrectly() throws SQLException, IllegalAccessException {
        Publisher publisher = new Publisher("Test Publisher");
        orManager.persist(publisher);

        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).row(0)
                           .value().isEqualTo(1)
                           .value().isEqualTo("Test Publisher");
    }


    @Test
    @DisplayName("Persist Book")
    void test_persist_Book_should_return_firstRowValuesCorrectly() throws SQLException, IllegalAccessException {
        Book book = new Book("Test Book", LocalDate.now());
        orManager.persist(book);

        Request request = new Request(dataSource, "select * from books");

        assertThat(request).row(0)
                           .value().isEqualTo(1)
                           .value().isEqualTo("Test Book")
                           .value().isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Find Publisher with id = 1")
    void test_findById_when_requesting_should_return_publisherObject_with_id1() {

        Request request = new Request(dataSource, "select * from publishers where id = 1");

        assertThat(request).column("id")
                           .value().isEqualTo(1)
                           .column("name")
                           .value().isEqualTo("Test Publisher");
    }

    @Test
    @DisplayName("Find Book with id = 1")
    void test_findById_when_requesting_should_return_bookObject_with_id1() {

        Request request = new Request(dataSource, "select * from books where id = 1");

        assertThat(request).column("id")
                           .value().isEqualTo(1)
                           .column("title")
                           .value().isEqualTo("Test Book")
                           .column("published_at")
                           .value().isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Find all books count in the table")
    void test_findAll_when_requestingAllBooks_should_returnNumberOfRowsCorrectly() throws Exception {
        List<Book> expected = orManager.findAll(Book.class);

        Request request = new Request(dataSource, "select * from books");

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    @DisplayName("Find all publishers count in the table")
    void test_findAll_when_requestingAllPublishers_should_returnNumberOfRowsCorrectly() throws Exception {
        List<Publisher> expected = orManager.findAll(Publisher.class);

        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    @DisplayName("Find all Books in column title")
    void test_findAll_when_requestingAllBooks_should_returnAllValues() throws Exception {

        Request request = new Request(dataSource, "select * from books");

        assertThat(request).column("title")
                           .containsValues("Test Book");
    }

    @Test
    @DisplayName("Find all Publishers in column name")
    void test_findAll_when_requestingAllPublishers_should_returnAllValues() throws Exception {

        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).column("name")
                           .containsValues("Test Publisher");
    }
}
