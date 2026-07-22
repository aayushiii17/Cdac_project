package com.collabcode.auth.exception;

/**
 * Thrown during registration when the requested email address already belongs
 * to an existing account.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
