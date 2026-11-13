package com.example.bankingapp.Account.Dtos;

import com.example.bankingapp.Account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AccountRequest(

   @NotNull @Positive
    Long userId,
   //Accept account number for now but later end up generating iti0000000000000
   @NotBlank @Size(max = 255)
    String accountNumber,

   @NotNull
   AccountType accountType

   ){
}
