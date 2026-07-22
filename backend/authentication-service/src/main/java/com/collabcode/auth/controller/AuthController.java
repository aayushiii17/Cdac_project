package com.collabcode.auth.controller;

import com.collabcode.auth.dto.auth.ApiResponse;
import com.collabcode.auth.dto.auth.AuthResponse;
import com.collabcode.auth.dto.auth.ChangePasswordRequest;
import com.collabcode.auth.dto.auth.ForgotPasswordRequest;
import com.collabcode.auth.dto.auth.LoginRequest;
import com.collabcode.auth.dto.auth.RefreshTokenRequest;
import com.collabcode.auth.dto.auth.ResetPasswordRequest;
import com.collabcode.auth.dto.auth.SignupRequest;
import com.collabcode.auth.dto.auth.UpdateProfileRequest;
import com.collabcode.auth.dto.auth.UserProfileResponse;
import com.collabcode.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes all authentication and account-management endpoints under {@code /api/v1/auth}.
 *
 * <p>Every method returns {@code ResponseEntity<ApiResponse<T>>} for a consistent
 * JSON envelope. Authenticated endpoints extract the principal via
 * {@code @AuthenticationPrincipal UserDetails}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/register
     * Public. Creates a new account; sends a welcome + verification email.
     * No tokens are returned – email must be verified first.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody SignupRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Registration successful. Please check your email to verify your account.",
                        response));
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/login
     * Public. Returns access token, refresh token, expiry, role, email, and name.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful.", response));
    }

    // ─── Email Verification ───────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/verify-email?token={token}
     * Public. Validates the email-verification token and marks the account as verified.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok(
                "Email verified successfully. You can now log in."));
    }

    /**
     * POST /api/v1/auth/resend-verification
     * Public. Regenerates and resends the email-verification link.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
                "Verification email sent. Please check your inbox."));
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/refresh-token
     * Public. Rotates the refresh token and returns a new access/refresh token pair.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully.", response));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/logout
     * Requires JWT. Revokes all refresh tokens for the authenticated user.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully."));
    }

    // ─── Forgot / Reset Password ──────────────────────────────────────────────

    /**
     * POST /api/v1/auth/forgot-password
     * Public. Sends a password-reset link if the email exists (generic response either way).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account with that email exists, a password reset link has been sent."));
    }

    /**
     * POST /api/v1/auth/reset-password
     * Public. Validates the reset token and sets the new BCrypt-encoded password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Password reset successfully. Please log in with your new password."));
    }

    // ─── Change Password ──────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/change-password
     * Requires JWT. Validates old password, sets new BCrypt-encoded password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully."));
    }

    // ─── User Profile ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/me
     * Requires JWT. Returns the authenticated user's profile (no password).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully.", profile));
    }

    /**
     * PUT /api/v1/auth/me
     * Requires JWT. Partial update of fullName, bio, avatarUrl.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully.", profile));
    }

    // ─── Account Management ───────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/deactivate
     * Requires JWT. Soft-deletes the account (isActive = false).
     */
    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated successfully."));
    }

    /**
     * POST /api/v1/auth/reactivate?email={email}
     * Public. Reactivates a deactivated account.
     */
    @PostMapping("/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateAccount(@RequestParam String email) {
        authService.reactivateAccount(email);
        return ResponseEntity.ok(ApiResponse.ok(
                "Account reactivated successfully. You can now log in."));
    }
}
