package org.example.lib;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.db.type.Request;
import org.assertj.db.type.Table;
import org.example.configs.HikariCPDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        dataSource = HikariCPDataSource.getHikariDatasourceConfiguration(new HikariDataSource());
        orManager = ORManager.withDataSource(new HikariDataSource());
    }

    @Test
    void test_when_tableDoesNotExist_should_return_tableDoesNotExist() {
        Table table = new Table(dataSource, "TEST");
        assertThat(table).doesNotExist();
    }

    @Test
    void test_when_tableBookExist_should_return_tableExist() {
        Table table = new Table(dataSource, "BOOK");
        assertThat(table).exists();
    }

    @Test
    void test_when_tablePublisherExist_should_return_tableExist() {
        Table table = new Table(dataSource, "PUBLISHER");
        assertThat(table).exists();
    }

    @Test
    void test_when_tableBookExist_should_return_membersCorrectly() {
        Table table = new Table(dataSource, "BOOK");
        assertThat(table).column(0).hasColumnName("id")
                .column(1).hasColumnName("title")
                .column(2).hasColumnName("published_at");
    }

    @Test
    void test_when_tablePublisherExist_should_return_membersCorrectly() {
        Table table = new Table(dataSource, "PUBLISHER");
        assertThat(table).column(0).hasColumnName("id")
                .column(1).hasColumnName("name");
    }

    @Test
    void test_findAll_when_requestingAllBooks_should_returnNumberOfRowsCorrectly() throws Exception {
        List<Book> expected = orManager.findAll(Book.class);

        Request request = new Request(dataSource, "select * from book");

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    void test_findAll_when_requestingAllPublishers_should_returnNumberOfRowsCorrectly() throws Exception {
        List<Publisher> expected = orManager.findAll(Publisher.class);

        Request request = new Request(dataSource, "select * from publisher");

        assertThat(request).hasNumberOfRows(expected.size());
    }

    @Test
    void test_findAll_when_requestingAllBooks_should_returnAllValues() throws Exception {

        Request request = new Request(dataSource, "select * from book");

        assertThat(request).column("title")
                .containsValues("Solaris",
                        "Just Book",
                        "Just Book 2",
                        "Just Book 3");
    }

    @Test
    void test_findAll_when_requestingAllPublishers_should_returnAllValues() throws Exception {

        Request request = new Request(dataSource, "select * from publisher");

        assertThat(request).column("name")
                .containsValues("Just Publisher",
                        "MyPub1",
                        "MyPub2",
                        "MyPub3");
    }

    @Test
    void test_persist_Publisher_object_into_database() throws SQLException, IllegalAccessException {

        Request request = new Request(dataSource, "select * from publishers");

        assertThat(request).row(0)
                .value().isEqualTo(1)
                .value().isEqualTo("Publisher_test_persist");
    }

    @Test
    void test_persist_Book_object_into_database() throws SQLException, IllegalAccessException {

        Request request = new Request(dataSource, "select * from books");

        assertThat(request).row(0)
                .value().isEqualTo(1)
                .value().isEqualTo("Test_Book_For_persist")
                .value().isEqualTo(LocalDate.of(1992, 1, 1));
    }

    @Test
    void test_findById_when_requesting_should_return_publisherObject_with_id1() {

        Request request = new Request(dataSource, "select * from publishers where id = 1");

        assertThat(request).column("id")
                .value().isEqualTo(1)
                .column("name")
                .value().isEqualTo("Publisher_test_persist");
    }

    @Test
    void test_findById_when_requesting_should_return_bookObject_with_id1() {

        Request request = new Request(dataSource, "select * from books where id = 1");

        assertThat(request).column("id")
                .value().isEqualTo(1)
                .column("title")
                .value().isEqualTo("Test_Book_For_persist")
                .column("published_at")
                .value().isEqualTo(LocalDate.of(1992, 1, 1));
    }
}
