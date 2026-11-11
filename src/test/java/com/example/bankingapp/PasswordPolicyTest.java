package com.example.bankingapp;

import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.Staff.StaffRequest;
import com.example.bankingapp.Staff.StaffRole;
import com.example.bankingapp.validation.PasswordValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordValidator validator = new PasswordValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"James!Lan", "JamesSmith", "James Smith", "James123!Lane",
            "James321!Lane", "James789!Lane", "James987!Lane", "JamesAbC!Lane",
            "JamesCbA!Lane", "JamesXyZ!Lane", "James1 2 3!Lane", "Jamesa b c!Lane",
            "James1\t2\t3!Lane", "James\u200BSmith"})
    void rejectsInvalidPasswords(String password) {
        assertFalse(validator.isValid(password, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"James!Lane", "JamesSmith!", "James-Smith", "James!13579", "James!11111", "James€Lane"})
    void acceptsNamesSpecialCharactersAndNonSequentialDigits(String password) {
        assertTrue(validator.isValid(password, null));
    }

    @Test
    void enforcesBcryptByteLimitWithoutCountingEmojiAsTwoCharacters() {
        assertTrue(validator.isValid("Q".repeat(71) + "!", null));
        assertFalse(validator.isValid("Q".repeat(72) + "!", null));
        assertTrue(validator.isValid("é".repeat(35) + "!!", null));
        assertFalse(validator.isValid("é".repeat(36) + "!", null));
        assertFalse(validator.isValid("😀".repeat(9), null));
        assertTrue(validator.isValid("😀".repeat(10), null));
    }

    @Test
    void requestsSharePolicyAndReportErrorsOnPasswordField() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var beanValidator = factory.getValidator();
            var customer = new UserRequest("james@example.com", "james", "JamesSmith!",
                    LocalDate.of(1990, 1, 1), "James", "Smith", 700);
            assertTrue(beanValidator.validate(customer).isEmpty());
            var staff = new StaffRequest("james.staff", "James!12345", StaffRole.ADMIN);
            var violations = beanValidator.validate(staff);
            assertEquals(1, violations.size());
            var violation = violations.iterator().next();
            assertEquals("password", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("at least 10 characters"));
            assertTrue(violation.getMessage().contains("special character"));
            assertTrue(violation.getMessage().contains("First and last names are allowed"));
        }
    }
}
