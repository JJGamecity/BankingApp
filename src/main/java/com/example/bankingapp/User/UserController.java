package com.example.bankingapp.User;

import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URL;

@RestController

@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        URI location = URI.create("/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    //Confirm on replace user putMapping
    @PutMapping("/replace-user/{id}")
    public ResponseEntity<Void> replaceUser(@PathVariable Long id,@Valid @RequestBody UserRequest request) {
        userService.replaceUser(id, request);
        return ResponseEntity.noContent().build();
    }

}
