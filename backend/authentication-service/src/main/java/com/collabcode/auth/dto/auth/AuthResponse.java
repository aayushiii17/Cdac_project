package com.collabcode.auth.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Returned by login and token-refresh operations.
 * On registration, only {@code email}, {@code name}, and {@code role} are populated
 * (no tokens are issued until email is verified).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    /** Always "Bearer" when tokens are present. */
    private String tokenType;

    /** Access token validity in milliseconds. */
    private long expiresIn;

    private String role;
    private String email;
    private String name;
}
