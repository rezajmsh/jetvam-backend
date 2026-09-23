package ir.jetvam.common.validation.validator;

import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.annotation.IranianPostalCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates values annotated with the iranian postal code constraint.
 * It delegates canonical identifier rules to the shared validator utilities.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class IranianPostalCodeValidator implements ConstraintValidator<IranianPostalCode, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || IranianIdentifiers.isValidPostalCode(value);
    }
}
