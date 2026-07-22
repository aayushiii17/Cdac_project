package com.collabcode.auth.exception;

/**
 * Thrown when a lookup by email or ID returns no results.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
