package com.example.bankingapp.Staff;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record StaffRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @NotBlank @Size(min = 15, max = 72) String password,
        @NotNull StaffRole role
) {
    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
