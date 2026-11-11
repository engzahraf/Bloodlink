package com.bloodlink.database.exceptions;

/**
 * Exception thrown when domain validation rules are violated.
 */
public class ValidationException extends DatabaseException {
    public ValidationException(String message) {
        super(message);
    }
}

