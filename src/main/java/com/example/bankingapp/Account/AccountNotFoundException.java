package com.example.bankingapp.Account;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id, String message) {
        super(message+" ID: "+id);
    }
    
    public AccountNotFoundException(Long id){
        super("Account with id "+id+" was not found.");
    }
}
