package com.collabcode.auth.exception;

/**
 * Thrown when the provided old password does not match the stored BCrypt hash
 * during a change-password operation.
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}
