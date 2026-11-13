package com.example.bankingapp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.bankingapp.Auth.SecurityResponses;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Single-instance credential-attempt limit. Do not trust client-supplied forwarding headers. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int attempts) { }
    private final Map<String, Window> windows = new HashMap<>();
    private final int limit;
    private final Clock clock;

    public LoginRateLimitFilter(int limit, Clock clock) {
        if (limit < 1) throw new IllegalArgumentException("Login limit must be positive");
        this.limit = limit;
        this.clock = clock;
    }

    private synchronized boolean allow(String address) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= 60_000);
        var window = windows.get(address);
        if (window == null) {
            // Bound memory without evicting active limits, which could permit bypass.
            if (windows.size() >= 10_000) return false;
            window = new Window(now, 0);
        }
        if (window.attempts() >= limit) return false;
        windows.put(address, new Window(window.start(), window.attempts() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        boolean credentials = ("POST".equals(request.getMethod())
                && ("/login".equals(request.getServletPath()) || "/auth/signup".equals(request.getServletPath())))
                || (authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6));
        if (credentials && !allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            SecurityResponses.error(response, 429, "RATE_LIMITED", "Too many authentication or signup attempts. Try again later.");
            return;
        }
        chain.doFilter(request, response);
    }
}
