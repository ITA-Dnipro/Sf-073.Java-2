package org.example.lib.utils;

public class ConstantsQueries {

    private ConstantsQueries() {
    }

    public static final class SqlSelectQueries {
        private SqlSelectQueries() {
        }

        public static final String SELECT_ALL_BOOKS = "SELECT * FROM book";
        public static final String SELECT_LAST_BOOK_ID = "SELECT book.id FROM book ORDER BY book.id DESC LIMIT 1";
    }

    public static final class SqlInsertQueries {

        private SqlInsertQueries() {}

        public static final String INSERT_ONE_ENTITY =
                "INSERT INTO ? (?) VALUES (?)";
    }
}
