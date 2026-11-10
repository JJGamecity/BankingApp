package com.example.bankingapp.User;

import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URL;
import java.util.List;

@RestController

@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        URI location = URI.create("/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> accountsList = userService.getAllUsers();
        return ResponseEntity.ok().body(accountsList);
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user =userService.getUserById(id);
        return ResponseEntity.ok().body(user);
    }




    //Confirm on replace user putMapping
    @PutMapping("/{id}")
    public ResponseEntity<Void> replaceUser(@PathVariable Long id,@Valid @RequestBody UserRequest request) {
        userService.replaceUser(id, request);
        return ResponseEntity.noContent().build();
    }

}
