package com.example.bankingapp.Auth;

import com.example.bankingapp.Staff.LoginNamePolicy;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserMapper;
import com.example.bankingapp.User.UserRepository;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.Locale;

@Service
@Validated
public class SignupService {
    private final UserRepository users;
    private final LoginNamePolicy names;
    private final PasswordEncoder passwords;
    private final UserMapper mapper;

    public SignupService(UserRepository users, LoginNamePolicy names, PasswordEncoder passwords, UserMapper mapper) {
        this.users = users;
        this.names = names;
        this.passwords = passwords;
        this.mapper = mapper;
    }

    @Transactional
    @PreAuthorize("!hasAnyRole('ADMIN', 'BANK_REPRESENTATIVE')")
    public UserResponse create(@Valid SignupRequest request) {
        names.requireAvailable(request.username(), null);
        String email = request.email().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new SignupConflictException("An account with this email address already exists. Sign in or use another email address.");
        }
        var customer = new User(email, passwords.encode(request.password()), request.username(),
                request.firstName().strip(), request.lastName().strip(), request.dateOfBirth(), null);
        return mapper.toResponseDto(users.saveAndFlush(customer));
    }
}
