package org.example.lib;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.db.type.Request;
import org.assertj.db.type.Source;
import org.example.entity.Book;
import org.example.lib.utils.Constants;
import org.example.lib.utils.ConstantsQueries;
import org.example.lib.utils.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.db.api.Assertions.assertThat;

class MethodSaveTest {

    private static final String dbURL = "jdbc:h2:file:D:\\0_SoftServe_Academy\\Sf-073.Java-2\\src\\main\\java\\org\\example\\db\\h2-test";
    private final Source h2Source = new Source(dbURL, "", "");
    private final Book book = new Book("Hibernate101", LocalDate.of(2000, 10, 05));
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

    MethodSaveTest() throws SQLException {}

    @Test
    void should_Check_If_Entity_Exists_In_Db() {
        Request requestAllBooks = new Request(h2Source, ConstantsQueries.SqlSelectQueries.SELECT_ALL_BOOKS);

        assertThat(requestAllBooks).row(0)
                .hasValues(1, "Solaris", "1961-01-01");
    }

    @Test
    void should_Check_If_Method_HasEntityId_Is_Correct() throws Exception {

        Assertions.assertFalse(EntityUtils.hasEntityId(book));
        List<Book> bookList = orManager.findAll(Book.class);
        Assertions.assertTrue(EntityUtils.hasEntityId(bookList.get(0)));

        try (PreparedStatement preparedStatement = orManagerImpl.getConnection()
                .prepareStatement(ConstantsQueries.SqlSelectQueries.SELECT_LAST_BOOK_ID)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            long lastRecordId = Long.parseLong(resultSet.getString(1));
            long newRecordId = lastRecordId + 1;
        }catch(SQLException ex){
            ex.printStackTrace();
        }

    }
}
