package ir.jetvam.common.validation.annotation;

import ir.jetvam.common.validation.validator.IranianNationalCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the Bean Validation constraint for an iranian national code value.
 * The annotation connects DTO validation to the shared validator implementation.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@Documented
@Constraint(validatedBy = IranianNationalCodeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IranianNationalCode {
    String message() default "{jetvam.validation.iranian-national-code}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
