package com.example.bankingapp.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
public @interface ValidPassword {
    String REQUIREMENTS = "Password must have at least 10 characters, include at least one special character "
            + "(such as !, @, # or -; spaces do not count), contain no ascending or descending sequence of "
            + "3 or more digits or English letters (such as 123, 321, abc or cba, including spaced sequences), "
            + "and be at most 72 UTF-8 bytes. First and last names are allowed if all other rules are met.";

    String message() default REQUIREMENTS;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
