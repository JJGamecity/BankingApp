package com.example.bankingapp;

import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserMapper;
import com.example.bankingapp.User.UserRepository;
import com.example.bankingapp.User.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPasswordTest {
    @Test
    void creatingAndReplacingCustomerStoresEncodedPasswords() {
        var repository = mock(UserRepository.class);
        var mapper = mock(UserMapper.class);
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(repository, mapper, encoder, mock(com.example.bankingapp.Staff.LoginNamePolicy.class));
        var dob = LocalDate.of(1990, 1, 1);
        var customer = new User("customer@example.com", "initial-password", "customer", "Test", "Customer", dob, 700);
        var create = new UserRequest("customer@example.com", "customer", "initial-password", dob, "Test", "Customer", 700);
        when(mapper.toEntity(create)).thenReturn(customer);

        service.createUser(create);

        assertNotEquals(create.password(), customer.getPassword());
        assertTrue(encoder.matches(create.password(), customer.getPassword()));
        verify(repository).save(customer);

        var replacement = new UserRequest("customer@example.com", "customer", "new-password", dob, "Test", "Customer", 700);
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        service.replaceUser(1L, replacement);

        assertTrue(encoder.matches(replacement.password(), customer.getPassword()));
        assertFalse(encoder.matches(create.password(), customer.getPassword()));
        verify(repository, times(2)).save(customer);
    }
}
