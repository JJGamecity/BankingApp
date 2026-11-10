package com.example.bankingapp.Staff;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<StaffAccount, Long> {
    Optional<StaffAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}
