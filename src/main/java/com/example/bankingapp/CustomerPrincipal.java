package com.example.bankingapp;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/** Carries the database identity so ownership never depends on a supplied username. */
public class CustomerPrincipal extends User {
    private final Long userId;

    public CustomerPrincipal(Long userId, String username, String encodedPassword) {
        super(username, encodedPassword, List.of(new SimpleGrantedAuthority("ROLE_" + SecurityConfig.CUSTOMER)));
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
