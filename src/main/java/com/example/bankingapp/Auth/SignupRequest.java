package com.example.bankingapp.Auth;

import com.example.bankingapp.Validation.ValidPassword;
import com.example.bankingapp.Validation.MinimumAge;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SignupRequest(
        @NotBlank(message = "Email address is required.")
        @Email(message = "Enter a valid email address, such as name@example.com.")
        @Size(max = 255, message = "Email address must be 255 characters or fewer.") String email,
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters.")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username can contain only letters, numbers, dots, underscores, and hyphens.") String username,
        @ValidPassword @Schema(description = ValidPassword.REQUIREMENTS,
                accessMode = Schema.AccessMode.WRITE_ONLY) String password,
        @NotBlank(message = "First name is required.")
        @Size(max = 100, message = "First name must be 100 characters or fewer.") String firstName,
        @NotBlank(message = "Last name is required.")
        @Size(max = 100, message = "Last name must be 100 characters or fewer.") String lastName,
        @NotNull(message = "Date of birth is required.")
        @Past(message = "Date of birth must be in the past.")
        @MinimumAge(18)
        @Schema(description = "Date of birth in YYYY-MM-DD format. You must be at least 18 years old to sign up.")
        LocalDate dateOfBirth) { }
