package com.example.bankingapp.Auth;

import com.example.bankingapp.CustomerPrincipal;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final SignupService signup;

    public AuthController(SignupService signup) { this.signup = signup; }

    public record SessionResponse(String username, Long customerId, List<String> roles,
                                  boolean signupRequired) { }

    @GetMapping("/me")
    public SessionResponse me(Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).sorted().toList();
        boolean pending = roles.contains("GOOGLE_PENDING");
        return new SessionResponse(pending ? null : authentication.getName(),
                authentication.getPrincipal() instanceof CustomerPrincipal customer ? customer.getUserId() : null,
                roles, pending);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        try {
            var customer = signup.create(request);
            // Signup creates a customer identity, not a bank account or an authenticated session.
            return ResponseEntity.created(URI.create("/users/me")).body(customer);
        } catch (DataIntegrityViolationException ex) {
            // A simultaneous signup can pass the availability check and then hit a unique constraint.
            throw new SignupConflictException("This username or email address is unavailable. Choose another username or email, or sign in if you already have an account.");
        }
    }
}
