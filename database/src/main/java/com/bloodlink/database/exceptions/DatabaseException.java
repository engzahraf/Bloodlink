package com.bloodlink.database.exceptions;

/**
 * Base runtime exception for database layer failures in BloodLink.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

