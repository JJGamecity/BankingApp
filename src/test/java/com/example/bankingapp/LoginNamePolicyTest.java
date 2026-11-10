package com.example.bankingapp;

import com.example.bankingapp.Staff.*;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginNamePolicyTest {
    @Test
    void customerCanKeepOwnNameButCannotTakeStaffOrOtherCustomerName() {
        var staff = mock(StaffRepository.class);
        var users = mock(UserRepository.class);
        var policy = new LoginNamePolicy(staff, users, "representative", "admin");
        var customer = mock(User.class);
        when(customer.getId()).thenReturn(1L);
        when(users.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(staff.existsByUsername("stored.admin")).thenReturn(true);

        assertDoesNotThrow(() -> policy.requireAvailable("customer", 1L));
        assertDoesNotThrow(() -> policy.requireAvailable("unused", null));
        assertThrows(UsernameAlreadyUsedException.class, () -> policy.requireAvailable("customer", 2L));
        assertThrows(UsernameAlreadyUsedException.class, () -> policy.requireAvailable("stored.admin", 1L));
        assertThrows(UsernameAlreadyUsedException.class, () -> policy.requireAvailable("ADMIN", 1L));
    }
}
