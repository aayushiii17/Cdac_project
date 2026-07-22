package com.collabcode.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a password meets all strength requirements defined by {@link StrongPassword}.
 *
 * <p>Pattern breakdown:
 * <pre>
 *   (?=.*[a-z])   – at least one lowercase letter
 *   (?=.*[A-Z])   – at least one uppercase letter
 *   (?=.*\d)      – at least one digit
 *   (?=.*[@$!%*?&]) – at least one special character
 *   [A-Za-z\d@$!%*?&]{8,} – minimum 8 allowed characters
 * </pre>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN);
    }
}
