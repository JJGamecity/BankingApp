package com.example.bankingapp.User;


//Exceptions(IOException, File Not Found can be recovered from before it even compiles while UnChecked exceptions cant (RunTime, Logic Exception)


public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User with the id "+id+" was not found");
    }
}
