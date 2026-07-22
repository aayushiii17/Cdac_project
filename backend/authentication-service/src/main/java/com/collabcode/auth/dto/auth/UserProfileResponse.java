package com.collabcode.auth.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Read-only projection of {@link com.collabcode.auth.entity.User} exposed via
 * {@code GET /api/v1/auth/me} and {@code PUT /api/v1/auth/me}.
 * The entity is never exposed directly.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;

    private boolean emailVerified;

    /** Serialised as "isActive" in JSON for clarity. */
    @JsonProperty("isActive")
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String bio;
    private String avatarUrl;
}
