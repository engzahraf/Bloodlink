package com.bloodlink.database.exceptions;

/**
 * Exception thrown when a requested database entity does not exist.
 */
public class RecordNotFoundException extends DatabaseException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}

