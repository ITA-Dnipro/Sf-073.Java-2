package org.example.lib.exception;

public class ORMException extends Exception {

    public ORMException(String custom_error_message) {
        super(custom_error_message);
    }
}
