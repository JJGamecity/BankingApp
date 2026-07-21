package com.example.bankingapp.User.DTOs;

import jakarta.persistence.Column;

public record UserResponse(
        Long id,

        String username,

        String email,

        String firstName,

        String lastName,

        Integer creditScore

) {
}
