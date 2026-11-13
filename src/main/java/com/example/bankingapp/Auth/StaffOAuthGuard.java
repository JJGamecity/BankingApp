package com.example.bankingapp.Auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** OAuth filters run before URL authorization, so protect identity changes before they run. */
public class StaffOAuthGuard extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        boolean googleLogin = path.equals("/oauth2/authorization/google") || path.equals("/login/oauth2/code/google");
        if (googleLogin) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean staff = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                            || authority.getAuthority().equals("ROLE_BANK_REPRESENTATIVE"));
            // OAuth browser navigation uses cookies. Basic/Bearer credentials must not bypass this guard
            // by being authenticated later in the filter chain, after OAuth already changed the session.
            if (staff || request.getHeader("Authorization") != null) {
                SecurityResponses.error(response, 403, "ACCESS_DENIED", "Google customer login is unavailable for this request. Use a signed-out browser session without an Authorization header.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
