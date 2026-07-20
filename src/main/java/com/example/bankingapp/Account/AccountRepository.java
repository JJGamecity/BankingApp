package com.example.bankingapp.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<com.example.demo.Account.Account, Long> {
}
