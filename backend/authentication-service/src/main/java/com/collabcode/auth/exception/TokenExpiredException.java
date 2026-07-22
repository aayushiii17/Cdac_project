package com.collabcode.auth.exception;

/**
 * Thrown when an email verification or password-reset token has passed its expiry date.
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
