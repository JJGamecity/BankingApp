package com.example.bankingapp.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = MinimumAgeValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
public @interface MinimumAge {
    int value() default 18;
    String message() default "You must be at least {value} years old to sign up.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
