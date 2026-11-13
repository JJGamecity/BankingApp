package com.example.bankingapp.Google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.bankingapp.validation.MinimumAge;
import java.time.LocalDate;

public record GoogleSignupRequest(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters.")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username can contain only letters, numbers, dots, underscores, and hyphens.") String username,
        @NotBlank(message = "First name is required.")
        @Size(max = 100, message = "First name must be 100 characters or fewer.") String firstName,
        @NotBlank(message = "Last name is required.")
        @Size(max = 100, message = "Last name must be 100 characters or fewer.") String lastName,
        @NotNull(message = "Date of birth is required.")
        @Past(message = "Date of birth must be in the past.") @MinimumAge(18)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth
) { }
