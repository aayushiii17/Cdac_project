package com.collabcode.auth.exception;

/**
 * Thrown when the supplied refresh token has expired or been revoked.
 * Client must re-authenticate (full login).
 */
public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
