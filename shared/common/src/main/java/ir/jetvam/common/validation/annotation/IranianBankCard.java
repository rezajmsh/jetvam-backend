package ir.jetvam.common.validation.annotation;

import ir.jetvam.common.validation.validator.IranianBankCardValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = IranianBankCardValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IranianBankCard {
    String message() default "{jetvam.validation.iranian-bank-card}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
