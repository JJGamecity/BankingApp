package com.example.bankingapp.Staff;

import com.example.bankingapp.User.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class LoginNamePolicy {
    private final StaffRepository staff;
    private final UserRepository users;
    private final String representative;
    private final String admin;

    public LoginNamePolicy(StaffRepository staff, UserRepository users,
                           @Value("${spring.security.user.name:}") String representative,
                           @Value("${bank.security.admin.username:}") String admin) {
        this.staff = staff;
        this.users = users;
        this.representative = representative;
        this.admin = admin;
    }

    public void requireAvailable(String username, Long currentCustomerId) {
        boolean existingCustomer = users.findByUsername(username)
                .map(user -> currentCustomerId == null || !Objects.equals(user.getId(), currentCustomerId))
                .orElse(false);
        if (username.equalsIgnoreCase(representative) || username.equalsIgnoreCase(admin)
                || staff.existsByUsername(username) || existingCustomer) {
            throw new UsernameAlreadyUsedException();
        }
    }
}
