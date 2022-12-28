package org.example.lib.utils;

public class ConstantsQueries {

    private ConstantsQueries() {
    }

    public static final class SqlSelectQueries {
        private SqlSelectQueries() {
        }

        public static final String SELECT_FIRST_BOOK = "SELECT * FROM BOOK LIMIT 1";
        public static final String SELECT_ALL_FROM_BOOK_BY_ID = "SELECT * FROM BOOK WHERE id=?";
    }

    public static final class SqlInsertQueries {

        private SqlInsertQueries() {}

    }

    public static final class SqlUpdateQueries {

        private SqlUpdateQueries() {}

    }
    public static final class SqlDeleteQueries {

        private SqlDeleteQueries() {}

    }
}
