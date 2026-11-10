package com.example.bankingapp.User.DTOs;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserRequest(

        @Column(nullable = false, unique = true)
        @Email(message="This is not a proper email.")
        @NotBlank
        String email,


        @NotBlank
        @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters")
        String username,

        //@CheckPassword
        @NotBlank
        @Size(min = 15, max = 72)
        String password,

        @NotNull
        LocalDate dateOfBirth,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotNull(message = "Credit score must be calculated before saving")
        @Min(value = 300, message = "Credit score can't be below 300")
        @Max(value = 850, message = "Credit score can't be over 850")
        Integer creditScore

) {
    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }
}
