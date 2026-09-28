package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.text.TextUtils;
import ir.jetvam.modules.identity.IdentityErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enforces the shared password length and whitespace requirements.
 * A single policy keeps provisioning, change and reset behavior consistent.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Component
public class DefaultPasswordPolicy implements PasswordPolicy {

    static final int MINIMUM_LENGTH = 10;
    static final int MAXIMUM_LENGTH = 200;

    @Override
    public String validate(String password) {
        if (!TextUtils.hasText(password)) {
            throw violation("Password must not be blank");
        }
        String value = password;
        if (value.length() < MINIMUM_LENGTH || value.length() > MAXIMUM_LENGTH) {
            throw violation("Password must contain between 10 and 200 characters");
        }
        if (!value.equals(value.strip())) {
            throw violation("Password must not start or end with whitespace");
        }
        return value;
    }

    private static ValidationException violation(String message) {
        return new ValidationException(IdentityErrorCode.PASSWORD_POLICY_VIOLATION, message, List.of());
    }
}
