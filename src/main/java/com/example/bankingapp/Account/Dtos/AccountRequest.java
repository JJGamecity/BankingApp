package com.example.bankingapp.Account.Dtos;

import com.example.bankingapp.Account.AccountType;
import com.example.bankingapp.User.User;
import jakarta.persistence.Column;

public record AccountRequest(

   @Column(nullable = false)
    Long userId,
   //Accept account number for now but later end up generating iti0000000000000
   @Column(nullable = false)
    String accountNumber,

   @Column(nullable = false)
   AccountType accountType

   ){
}
