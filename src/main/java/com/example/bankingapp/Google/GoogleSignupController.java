package com.example.bankingapp.Google;

import com.example.bankingapp.Staff.UsernameAlreadyUsedException;
import com.example.bankingapp.Auth.SignupConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@ConditionalOnProperty(name = "bank.security.google.enabled", havingValue = "true")
@PreAuthorize("hasRole('GOOGLE_PENDING')")
public class GoogleSignupController {
    private final GoogleSignupService signup;

    public GoogleSignupController(GoogleSignupService signup) { this.signup = signup; }

    @GetMapping(value = "/signup/google", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(@AuthenticationPrincipal OidcUser principal, CsrfToken csrf,
                                       HttpServletRequest request) {
        GoogleSignupService.verifiedSubject(principal);
        return page(200, principal, csrf, request, "", principal.getIdToken().getGivenName(),
                principal.getIdToken().getFamilyName(), "", List.of());
    }

    @PostMapping(value = "/signup/google", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> complete(@AuthenticationPrincipal OidcUser principal,
            @Valid @ModelAttribute GoogleSignupRequest profile, BindingResult errors, CsrfToken csrf,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        GoogleSignupService.verifiedSubject(principal);
        if (errors.hasErrors()) {
            return page(400, principal, csrf, request, profile.username(), profile.firstName(), profile.lastName(),
                    request.getParameter("dateOfBirth"), errors.getFieldErrors().stream()
                            .map(error -> error.getField() + ": " + (error.isBindingFailure()
                                    && error.getField().equals("dateOfBirth")
                                    ? "Enter a valid date of birth in YYYY-MM-DD format." : error.getDefaultMessage())).toList());
        }
        try {
            var customer = signup.complete(principal, profile);
            GoogleCustomerSession.signIn(customer, request, response);
            response.sendRedirect(request.getContextPath() + "/accounts/me");
            return null;
        } catch (UsernameAlreadyUsedException | SignupConflictException ex) {
            return page(409, principal, csrf, request, profile.username(), profile.firstName(), profile.lastName(),
                    request.getParameter("dateOfBirth"), List.of(ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return page(409, principal, csrf, request, profile.username(), profile.firstName(), profile.lastName(),
                    request.getParameter("dateOfBirth"), List.of("Could not create this account. Choose another username, "
                            + "or contact the bank if you already have an account. Existing accounts are not automatically linked."));
        }
    }

    private ResponseEntity<String> page(int status, OidcUser principal, CsrfToken csrf, HttpServletRequest request,
            String username, String first, String last, String dob, List<String> errors) {
        String messages = errors.stream().map(error -> "<li>" + escape(error) + "</li>")
                .reduce("", String::concat);
        String html = """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Complete your customer profile</title></head><body><main>
                <h1>Complete your customer profile</h1>
                <p>Google email: <strong>%s</strong></p>
                <p>Confirm your first and last name and enter your date of birth. All fields are required.</p>
                <p>You must be at least 18 years old to sign up.</p>
                <p>Your Google login replaces a password for this account. Creating a profile does not open a bank account.</p>
                <ul role="alert">%s</ul>
                <form method="post" action="%s/signup/google">
                <input type="hidden" name="%s" value="%s">
                <p><label>Username <input name="username" required minlength="3" maxlength="15" pattern="[A-Za-z0-9._-]+" autocomplete="username" value="%s"></label></p>
                <p>Use 3–15 letters, digits, dots, underscores or hyphens.</p>
                <p><label>First name <input name="firstName" required maxlength="100" autocomplete="given-name" value="%s"></label></p>
                <p><label>Last name <input name="lastName" required maxlength="100" autocomplete="family-name" value="%s"></label></p>
                <p><label>Date of birth <input type="date" name="dateOfBirth" required autocomplete="bday" max="%s" value="%s"></label></p>
                <button type="submit">Create customer profile</button>
                </form></main></body></html>
                """.formatted(escape(principal.getIdToken().getEmail()), messages, escape(request.getContextPath()),
                escape(csrf.getParameterName()), escape(csrf.getToken()), escape(username), escape(first), escape(last),
                LocalDate.now().minusYears(18), escape(dob));
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
    }

    private static String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
