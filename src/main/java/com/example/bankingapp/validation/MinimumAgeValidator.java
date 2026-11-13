package com.example.bankingapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {
    private int minimumAge;

    @Override public void initialize(MinimumAge constraint) { minimumAge = constraint.value(); }

    @Override public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        // Let @NotNull report a missing DOB. Use the validation clock for deterministic boundaries.
        if (dateOfBirth == null) return true;
        LocalDate latestAllowedBirthDate = LocalDate.now(context.getClockProvider().getClock()).minusYears(minimumAge);
        return !dateOfBirth.isAfter(latestAllowedBirthDate);
    }
}
