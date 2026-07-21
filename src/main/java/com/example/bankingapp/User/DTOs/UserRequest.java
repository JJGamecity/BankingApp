package com.example.bankingapp.User.DTOs;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserRequest(

        @Column(nullable = false, unique = true)
        @Email(message="This is not a proper email.")
        String email,

        @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters")
        @Column(nullable = false, unique = true)
        String username,

        //@CheckPassword
        @Column(nullable = false)
        String password,

        @Column(nullable = false)
        LocalDate dateOfBirth,

        @Column(nullable = false)
        String firstName,

        @Column(nullable = false)
        String lastName,

        @NotNull(message = "Credit score must be calculated before saving")
        @Min(value = 300, message = "Credit score can't be below 300")
        @Max(value = 850, message = "Credit score can't be over 850")
        Integer creditScore

) {



}
