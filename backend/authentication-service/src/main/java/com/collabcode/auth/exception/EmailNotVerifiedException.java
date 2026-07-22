package com.collabcode.auth.exception;

/**
 * Thrown when a user attempts to log in before verifying their email address.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
