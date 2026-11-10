package com.example.bankingapp.Staff;

public class UsernameAlreadyUsedException extends RuntimeException {
    public UsernameAlreadyUsedException() {
        super("Username is already in use or reserved for a configured staff login.");
    }
}
