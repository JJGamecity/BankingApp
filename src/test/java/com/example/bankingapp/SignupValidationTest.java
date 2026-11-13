package com.example.bankingapp;

import com.example.bankingapp.Auth.AuthController;
import com.example.bankingapp.Auth.SignupRequest;
import com.example.bankingapp.Auth.SignupService;
import com.example.bankingapp.Google.GoogleSignupRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignupValidationTest {
    @ParameterizedTest
    @CsvSource({
            "2026-11-13,2008-11-13,true", // Eighteenth birthday is allowed.
            "2026-11-13,2008-11-14,false", // One day too young.
            "2026-11-13,2008-11-12,true",
            "2026-11-13,2026-11-14,false",
            "2026-02-28,2008-02-29,false", // Leap-day birthday reaches the cutoff on March 1.
            "2026-03-01,2008-02-29,true"
    })
    void bothSignupMethodsUseTheSameAgeBoundary(String today, String birthday, boolean valid) {
        var clock = Clock.fixed(LocalDate.parse(today).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        try (var factory = Validation.byDefaultProvider().configure().clockProvider(() -> clock).buildValidatorFactory()) {
            var validator = factory.getValidator();
            var dob = LocalDate.parse(birthday);
            var passwordSignup = new SignupRequest("jane@example.test", "jane", "Jane!Smith", "Jane", "Smith", dob);
            var googleSignup = new GoogleSignupRequest("jane", "Jane", "Smith", dob);
            assertEquals(valid, validator.validate(passwordSignup).isEmpty());
            assertEquals(valid, validator.validate(googleSignup).isEmpty());
            if (!valid) {
                assertTrue(validator.validate(passwordSignup).stream().anyMatch(violation -> violation.getMessage()
                        .equals("You must be at least 18 years old to sign up.")));
            }
        }
    }

    @Test void missingDateOfBirthHasARequiredMessageWithoutAnAgeError() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new SignupRequest("jane@example.test", "jane", "Jane!Smith", "Jane", "Smith", null);
            var violations = factory.getValidator().validate(request);
            assertEquals(1, violations.size());
            assertEquals("Date of birth is required.", violations.iterator().next().getMessage());
        }
    }

    @Test void publicSignupReturnsHelpfulMessagesWithoutCreatingAnAccount() throws Exception {
        var service = mock(SignupService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new MainExceptionHandler()).build();
        mvc.perform(post("/auth/signup").contentType("application/json").content("""
                {"email":"invalid-email","username":"bad username","password":"short",
                 "firstName":"","lastName":" ","dateOfBirth":"%s"}
                """.formatted(LocalDate.now().minusYears(17))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasItems(
                        "email: Enter a valid email address, such as name@example.com.",
                        "username: Username can contain only letters, numbers, dots, underscores, and hyphens.",
                        "firstName: First name is required.", "lastName: Last name is required.",
                        "dateOfBirth: You must be at least 18 years old to sign up.")));
        verifyNoInteractions(service);
    }

    @Test void malformedBirthDateReturnsAnActionableFormatMessage() throws Exception {
        var service = mock(SignupService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new MainExceptionHandler()).build();
        mvc.perform(post("/auth/signup").contentType("application/json").content("""
                {"email":"jane@example.test","username":"jane","password":"Jane!Smith",
                 "firstName":"Jane","lastName":"Smith","dateOfBirth":"2000-02-30"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors", hasItem(
                        "dateOfBirth: Enter a valid date of birth in YYYY-MM-DD format.")));
        verifyNoInteractions(service);
    }

    @Test void concurrentSignupConflictDoesNotExposeDatabaseDetails() throws Exception {
        var service = mock(SignupService.class);
        when(service.create(any())).thenThrow(new DataIntegrityViolationException("private SQL details"));
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new MainExceptionHandler()).build();
        mvc.perform(post("/auth/signup").contentType("application/json").content("""
                {"email":"jane@example.test","username":"jane","password":"Jane!Smith",
                 "firstName":"Jane","lastName":"Smith","dateOfBirth":"1990-02-03"}
                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SIGNUP_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("This username or email address is unavailable. Choose another username or email, or sign in if you already have an account."));
    }
}
