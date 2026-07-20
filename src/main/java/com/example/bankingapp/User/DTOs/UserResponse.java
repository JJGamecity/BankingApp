package com.example.bankingapp.User.DTOs;

public record UserResponse(
        Long id,
        String username,
        String email
) {
}
