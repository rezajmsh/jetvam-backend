package ir.jetvam.common.validation.validator;

import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.annotation.IranianNationalCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates values annotated with the iranian national code constraint.
 * It delegates canonical identifier rules to the shared validator utilities.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class IranianNationalCodeValidator implements ConstraintValidator<IranianNationalCode, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || IranianIdentifiers.isValidNationalCode(value);
    }
}
