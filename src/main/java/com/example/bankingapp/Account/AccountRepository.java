package com.example.bankingapp.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdOrderByIdAsc(Long id);
    java.util.Optional<Account> findByIdAndUserId(Long accountId, Long userId);
}
