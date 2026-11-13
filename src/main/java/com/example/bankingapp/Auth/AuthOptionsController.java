package com.example.bankingapp.Auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthOptionsController {
    private final boolean googleEnabled;

    public AuthOptionsController(@Value("${bank.security.google.enabled:false}") boolean googleEnabled) {
        this.googleEnabled = googleEnabled;
    }

    public record AuthOptions(boolean googleSignInEnabled) { }

    @GetMapping("/auth/options")
    public AuthOptions options() { return new AuthOptions(googleEnabled); }
}
