package com.collabcode.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.collabcode.auth.entity.RefreshToken;
import com.collabcode.auth.entity.User;

/**
 * DAO for refresh token persistence.
 * Spring Data JPA derives all query implementations at runtime.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Used during token rotation and logout to clear all existing tokens for a user.
     * Spring Data JPA wraps this derived delete in a transaction automatically.
     */
    void deleteByUser(User user);
}
