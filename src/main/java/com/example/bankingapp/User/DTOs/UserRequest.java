package com.example.bankingapp.User.DTOs;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRequest(
        @Column(nullable = false, unique = true)
        @Email(message="This is not a proper email.")
        String email,
        @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters")
        @Column(nullable = false, unique = true)
        String username,
        @Column(nullable = false)
        LocalDate dateOfBirth) {



}
