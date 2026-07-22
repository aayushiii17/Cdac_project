package com.collabcode.auth.service;

import com.collabcode.auth.dto.auth.AuthResponse;
import com.collabcode.auth.dto.auth.ChangePasswordRequest;
import com.collabcode.auth.dto.auth.LoginRequest;
import com.collabcode.auth.dto.auth.ResetPasswordRequest;
import com.collabcode.auth.dto.auth.SignupRequest;
import com.collabcode.auth.dto.auth.UpdateProfileRequest;
import com.collabcode.auth.dto.auth.UserProfileResponse;
import com.collabcode.auth.entity.RefreshToken;
import com.collabcode.auth.entity.Role;
import com.collabcode.auth.entity.User;
import com.collabcode.auth.exception.AccountDeactivatedException;
import com.collabcode.auth.exception.DuplicateEmailException;
import com.collabcode.auth.exception.EmailNotVerifiedException;
import com.collabcode.auth.exception.InvalidTokenException;
import com.collabcode.auth.exception.PasswordMismatchException;
import com.collabcode.auth.exception.TokenExpiredException;
import com.collabcode.auth.exception.UserNotFoundException;
import com.collabcode.auth.repository.UserRepository;
import com.collabcode.auth.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Central authentication service handling registration, login, email verification,
 * token management, password operations, profile management, and account lifecycle.
 *
 * <p>{@code AuthenticationManager} is injected lazily to avoid a circular dependency
 * with {@link com.collabcode.auth.config.SecurityConfig}.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       EmailService emailService,
                       CustomUserDetailsService userDetailsService,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Registers a new user, persists them with {@code emailVerified=false}, and sends a
     * welcome email containing the email-verification link.
     *
     * <p>No tokens are issued at this point; the user must verify their email first.
     *
     * @throws DuplicateEmailException if the email is already in use
     */
    @Transactional
    public AuthResponse register(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .active(true)
                .emailVerificationToken(verificationToken)
                .emailVerificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        user = userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(), verificationToken);
        } catch (Exception ex) {
            log.warn("Welcome email failed for {}: {}", user.getEmail(), ex.getMessage());
            // Do not roll back registration on email delivery failure
        }

        return AuthResponse.builder()
                .email(user.getEmail())
                .name(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Authenticates credentials, enforces email-verified and active checks, updates
     * {@code lastLogin}, and returns a fresh access/refresh token pair.
     *
     * @throws EmailNotVerifiedException  if the user has not verified their email
     * @throws AccountDeactivatedException if the account is deactivated
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegate credential check to Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(
                    "Please verify your email address before logging in.");
        }

        if (!user.isActive()) {
            throw new AccountDeactivatedException(
                    "Your account is deactivated. Please contact support to reactivate.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .role(user.getRole().name())
                .email(user.getEmail())
                .name(user.getFullName())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates the one-time email verification token and marks the account as verified.
     *
     * @throws InvalidTokenException   if the token does not exist / was already used
     * @throws TokenExpiredException   if the token is past its 24-hour window
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "Invalid or already-used email verification token."));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException(
                    "Email verification token has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    /**
     * Generates a fresh verification token and re-sends the verification email.
     *
     * @throws UserNotFoundException if no account exists for the email
     * @throws InvalidTokenException if the email is already verified
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        if (user.isEmailVerified()) {
            throw new InvalidTokenException("This email address has already been verified.");
        }

        String newToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), newToken);
        } catch (Exception ex) {
            log.warn("Resend verification email failed for {}: {}", email, ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates the supplied refresh token, rotates it (old token deleted, new one created),
     * and returns a fresh access/refresh token pair.
     *
     * @throws com.collabcode.auth.exception.RefreshTokenExpiredException if expired/revoked
     * @throws InvalidTokenException if the token is not found
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        if (!user.isActive()) {
            throw new AccountDeactivatedException("Account is deactivated.");
        }

        // Rotate: old token deleted inside createRefreshToken, new one created
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .role(user.getRole().name())
                .email(user.getEmail())
                .name(user.getFullName())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Revokes all refresh tokens for the authenticated user.
     * The access token is stateless and will expire on its own.
     */
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        refreshTokenService.revokeAllUserTokens(user);
        log.info("User logged out: {}", email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORGOT / RESET PASSWORD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a 1-hour password-reset token and emails a reset link.
     *
     * <p>If no account exists for the email, the method returns silently to prevent
     * user enumeration. The controller always returns the same generic message.
     */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            log.warn("Forgot-password requested for unknown email: {}", email);
            return; // Silent return – prevent user enumeration
        }

        User user = optionalUser.get();
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);
        } catch (Exception ex) {
            log.warn("Password-reset email failed for {}: {}", email, ex.getMessage());
        }
    }

    /**
     * Validates the reset token, BCrypt-encodes the new password, clears the token,
     * and revokes all refresh tokens to force re-login.
     *
     * @throws InvalidTokenException  if the token is not found / already used
     * @throws TokenExpiredException  if the token is past its 1-hour window
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException(
                        "Invalid or already-used password reset token."));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException(
                    "Password reset token has expired. Please request a new reset link.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);
        log.info("Password reset successful for user: {}", user.getEmail());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHANGE PASSWORD (authenticated)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies the old password, encodes and saves the new one, then revokes
     * all refresh tokens to force re-authentication on other devices.
     *
     * @throws PasswordMismatchException if {@code oldPassword} does not match the stored hash
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new PasswordMismatchException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);
        log.info("Password changed for user: {}", email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER PROFILE
    // ═══════════════════════════════════════════════════════════════════════════

    /** Returns the profile of the currently authenticated user as a DTO (no password). */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        return mapToProfileResponse(user);
    }

    /**
     * Applies partial updates (only non-null/non-blank fields are changed).
     * Email and role cannot be changed via this endpoint.
     */
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return mapToProfileResponse(userRepository.save(user));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCOUNT MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Soft-deletes the account by setting {@code active=false} and revoking all tokens.
     */
    @Transactional
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);
        log.info("Account deactivated for user: {}", email);
    }

    /**
     * Reactivates a previously deactivated account.
     *
     * @throws InvalidTokenException if the account is already active
     */
    @Transactional
    public void reactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        if (user.isActive()) {
            throw new InvalidTokenException("This account is already active.");
        }

        user.setActive(true);
        userRepository.save(user);
        log.info("Account reactivated for user: {}", email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
