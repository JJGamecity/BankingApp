package com.example.bankingapp.Staff;

import jakarta.validation.constraints.*;
import com.example.bankingapp.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;

public record StaffRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @ValidPassword
        @Schema(description = ValidPassword.REQUIREMENTS, accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,
        @NotNull StaffRole role
) {
}
