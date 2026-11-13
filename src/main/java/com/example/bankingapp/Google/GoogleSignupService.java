package com.example.bankingapp.Google;

import com.example.bankingapp.Staff.LoginNamePolicy;
import com.example.bankingapp.Auth.SignupConflictException;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.Instant;
import java.util.Optional;

@Service
@Validated
public class GoogleSignupService {
    private final UserRepository users;
    private final LoginNamePolicy names;

    public GoogleSignupService(UserRepository users, LoginNamePolicy names) {
        this.users = users;
        this.names = names;
    }

    public static String verifiedSubject(OidcUser principal) {
        var token = principal.getIdToken();
        String issuer = token.getClaimAsString("iss");
        if (!("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))
                || !Boolean.TRUE.equals(token.getEmailVerified())
                || token.getEmail() == null || token.getEmail().isBlank()
                || token.getSubject() == null || token.getSubject().isBlank()
                || token.getExpiresAt() == null || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new AccessDeniedException("A current, verified Google login is required");
        }
        return token.getSubject();
    }

    @Transactional(readOnly = true)
    public Optional<User> findCustomer(OidcUser principal) {
        return users.findByGoogleSubject(verifiedSubject(principal));
    }

    @Transactional
    @PreAuthorize("hasRole('GOOGLE_PENDING')")
    public User complete(OidcUser principal, @Valid GoogleSignupRequest request) {
        String subject = verifiedSubject(principal);
        var existing = users.findByGoogleSubject(subject);
        if (existing.isPresent()) return existing.get();
        String email = principal.getIdToken().getEmail();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new SignupConflictException("An account with this Google email already exists. Sign in with your existing login or contact the bank; accounts are not linked automatically.");
        }
        names.requireAvailable(request.username(), null);
        var customer = new User(email, null, request.username(), request.firstName().strip(),
                request.lastName().strip(), request.dateOfBirth(), null);
        customer.linkGoogleSubject(subject);
        return users.saveAndFlush(customer);
    }
}
