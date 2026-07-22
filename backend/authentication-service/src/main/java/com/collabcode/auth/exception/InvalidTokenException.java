package com.collabcode.auth.exception;

/**
 * Thrown when a supplied token (email verification, password-reset, etc.)
 * does not match any valid record in the database.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
