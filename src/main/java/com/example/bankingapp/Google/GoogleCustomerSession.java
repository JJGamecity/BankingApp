package com.example.bankingapp.Google;

import com.example.bankingapp.CustomerPrincipal;
import com.example.bankingapp.User.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

public final class GoogleCustomerSession {
    private GoogleCustomerSession() { }

    public static void signIn(User customer, HttpServletRequest request, HttpServletResponse response) {
        // Google accounts have no local password; this placeholder is never used for password authentication.
        var principal = new CustomerPrincipal(customer.getId(), customer.getUsername(), "GOOGLE_ONLY");
        principal.eraseCredentials();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        request.getSession();
        request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        new HttpSessionCsrfTokenRepository().saveToken(null, request, response);
    }
}
