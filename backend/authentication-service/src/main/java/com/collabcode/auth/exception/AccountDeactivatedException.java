package com.collabcode.auth.exception;

/**
 * Thrown when a user whose account has been deactivated (isActive=false)
 * attempts an action that requires an active account.
 */
public class AccountDeactivatedException extends RuntimeException {

    public AccountDeactivatedException(String message) {
        super(message);
    }
}
