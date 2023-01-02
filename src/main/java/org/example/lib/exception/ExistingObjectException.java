package org.example.lib.exception;

import java.sql.SQLException;

public class ExistingObjectException extends SQLException {
    public ExistingObjectException(String exceptionString) {
        super(exceptionString);
    }
}
