package org.example.lib;

import org.assertj.db.type.Request;
import org.assertj.db.type.Table;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.exception.ORMException;
import org.example.lib.utils.DBUtils;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.db.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ORManagerImplTest {
    private static DataSource dataSource;
    private static ORManager orManager;


    @BeforeAll
    static void setUp() throws SQLException, ORMException {
        DBUtils.init();
        dataSource = DBUtils.getDataSource();
        orManager = DBUtils.getOrManager();
    }

    @AfterAll
    static void clearDB() throws SQLException {
        DBUtils.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Non existing table")
    void test_when_tableDoesNotExist_should_return_tableDoesNotExist() {
        Table table = new Table(dataSource, "tests");

        assertThat(table).doesNotExist();
    }

    @Test
    @Order(2)
    @DisplayName("Returns all Books Columns correctly")
    void test_when_tableBookExist_should_return_columnNamesCorrectly() {
        Table table = new Table(dataSource, "books");

        assertThat(table).column(0).hasColumnName("id")
                         .column(1).hasColumnName("title")
                         .column(2).hasColumnName("published_at")
                         .column(3).hasColumnName("publisher_id");
    }

    @Test
    @Order(3)
    @DisplayName("Returns all Publishers Columns correctly")
    void test_when_tablePublisherExist_should_return_columnNamesCorrectly() {
        Table table = new Table(dataSource, "publishers");

        assertThat(table).column(0).hasColumnName("id")
                         .column(1).hasColumnName("name");
    }

    @Test
    @Order(4)
    @DisplayName("Persisted publisher exists")
    void test_persistedPublisher_should_return_firstRowValuesCorrectly() {
        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).row(0)
                           .value().isEqualTo(1)
                           .value().isEqualTo("Test Publisher");
    }

    @Test
    @Order(5)
    @DisplayName("Persisted book exists")
    void test_persistedBook_should_return_firstRowValuesCorrectly() {
        Request request = new Request(dataSource, "select * from books");

        assertThat(request).row(0)
                           .value().isEqualTo(1)
                           .value().isEqualTo("Test Book")
                           .value().isEqualTo(LocalDate.now());
    }

    @Test
    @Order(6)
    @DisplayName("Find Publisher with id = 1")
    void test_findById_when_requesting_should_return_publisherObject_with_id1() throws ORMException {
        Request request = new Request(dataSource, "select * from publishers where id = 1");

        Publisher publisher = orManager.findById(1, Publisher.class).get();

        assertThat(request).column("id")
                           .value().isEqualTo(publisher.getId())
                           .column("name")
                           .value().isEqualTo("Test Publisher");
    }

    @Test
    @Order(7)
    @DisplayName("Find Book with id = 1")
    void test_findById_when_requesting_should_return_bookObject_with_id1() throws ORMException {
        Request request = new Request(dataSource, "select * from books where id = 1");

        Book book = orManager.findById(1, Book.class).get();

        assertThat(request).column("id")
                           .value().isEqualTo(book.getId())
                           .column("title")
                           .value().isEqualTo("Test Book")
                           .column("published_at")
                           .value().isEqualTo(LocalDate.now());
    }

    @Test
    @Order(8)
    @DisplayName("Find all books count in the table")
    void test_findAll_when_requestingAllBooks_should_returnNumberOfRowsCorrectly() throws ORMException {
        Request request = new Request(dataSource, "select * from books");

        List<Book> expected = orManager.findAll(Book.class);

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    @Order(9)
    @DisplayName("Find all publishers count in the table")
    void test_findAll_when_requestingAllPublishers_should_returnNumberOfRowsCorrectly() throws ORMException {
        Request request = new Request(dataSource, "select * from publishers");

        List<Publisher> expected = orManager.findAll(Publisher.class);

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    @Order(10)
    @DisplayName("Find all books")
    void test_findAll_when_requestingAllBooks_should_return_AllBooksCorrectly() throws ORMException {
        Request request = new Request(dataSource, "select * from books");

        List<Book> expected = orManager.findAll(Book.class);

        assertThat(request).equals(expected);
    }

    @Test
    @Order(11)
    @DisplayName("Find all publishers")
    void test_findAll_when_requestingAllPublishers_should_return_AllBooksCorrectly() throws ORMException {
        Request request = new Request(dataSource, "select * from publishers");

        List<Publisher> expected = orManager.findAll(Publisher.class);

        assertThat(request).equals(expected);
    }

    @Test
    @Order(12)
    @DisplayName("Find all Books in column title")
    void test_findAll_when_requestingAllBooks_should_returnAllValues() {

        Request request = new Request(dataSource, "select * from books");

        assertThat(request).column("title")
                           .containsValues("Test Book", "Test book 2");
    }

    @Test
    @Order(13)
    @DisplayName("Find all Publishers in column name")
    void test_findAll_when_requestingAllPublishers_should_returnAllValues() {
        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).column("name")
                           .containsValues("Test Publisher", "Test Publisher 2");
    }

    @Test
    @Order(14)
    @DisplayName("Delete Book with id 1")
    void test_delete_bookWith_id1() throws ORMException {
        Book book = orManager.findById(1, Book.class).get();
        orManager.delete(book);
        Request request = new Request(dataSource, "select * from books where id = 1");

        assertThat(request).isEmpty();
    }

    @Test
    @Order(15)
    @DisplayName("Delete Publisher with id 1")
    void test_delete_publisherWith_id1() throws ORMException {
        Publisher publisher = orManager.findById(1, Publisher.class).get();
        orManager.delete(publisher);
        Request request = new Request(dataSource, "select * from publishers where id = 1");

        assertThat(request).isEmpty();
    }

    @Test
    @Order(16)
    @DisplayName("Refresh Book with id = 2")
    void test_refresh_when_refreshingBookWithIdTwo_should_returnRefreshedBook() throws ORMException {
        Request request = new Request(dataSource, "select * from books where id = 2");

        Book book = orManager.findById(2, Book.class).get();
        book.setTitle("new title");
        Book refresh = orManager.refresh(book);

        assertThat(request).equals(refresh);
    }

    @Test
    @Order(17)
    @DisplayName("Refresh Publisher with id = 2")
    void test_refresh_when_refreshingPublisherWithIdTwo_should_returnRefreshedPublisher() throws ORMException {
        Request request = new Request(dataSource, "select * from publishers where id = 2");

        Publisher publisher = orManager.findById(2, Publisher.class).get();
        publisher.setName("new name");
        Publisher refresh = orManager.refresh(publisher);

        assertThat(request).equals(refresh);
    }
}
