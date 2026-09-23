package ir.jetvam.common.validation.validator;

import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.annotation.IranianMobileNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates values annotated with the iranian mobile number constraint.
 * It delegates canonical identifier rules to the shared validator utilities.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class IranianMobileNumberValidator implements ConstraintValidator<IranianMobileNumber, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || IranianIdentifiers.isValidMobileNumber(value);
    }
}
