package com.collabcode.auth.service;

import com.collabcode.auth.entity.RefreshToken;
import com.collabcode.auth.entity.User;
import com.collabcode.auth.exception.InvalidTokenException;
import com.collabcode.auth.exception.RefreshTokenExpiredException;
import com.collabcode.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages the lifecycle of refresh tokens:
 * creation, look-up, expiry verification, rotation (revoke-all + create), and revocation.
 *
 * <p>Rotation policy: when a new refresh token is issued, all previous tokens for the user
 * are deleted. This ensures a refresh token can only be used once (prevents replay).
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${app.jwt.refresh-expiration}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    /**
     * Revokes all existing tokens for the user, then persists a fresh UUID token.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // ─── Find ─────────────────────────────────────────────────────────────────

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found. Please log in again."));
    }

    // ─── Verification ─────────────────────────────────────────────────────────

    /**
     * Checks that the token has not been revoked and has not expired.
     * Deletes the expired token from DB before throwing.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException(
                    "Refresh token has been revoked. Please log in again.");
        }
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException(
                    "Refresh token has expired. Please log in again.");
        }
        return token;
    }

    // ─── Revoke ───────────────────────────────────────────────────────────────

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Removes ALL refresh tokens for the user (used on logout, password change, deactivation).
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
