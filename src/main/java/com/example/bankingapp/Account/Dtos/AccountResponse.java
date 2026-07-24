package com.example.bankingapp.Account.Dtos;



import com.example.bankingapp.Account.AccountType;

import java.math.BigDecimal;

public record AccountResponse (
    Long id,
    Long userId,
    AccountType accountType,
    String accountNumber,
    String routingNumber,
    BigDecimal availableBalance

){
}
