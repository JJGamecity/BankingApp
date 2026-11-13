package com.example.bankingapp.Auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;

/** Security-filter failures happen before controller exception handlers. */
public final class SecurityResponses {
    private SecurityResponses() { }

    public static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("json");
    }

    public static void error(HttpServletResponse response, int status, String code, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        // Only fixed, server-owned strings may be passed here.
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":" + status
                + ",\"title\":\"" + HttpStatus.valueOf(status).getReasonPhrase() + "\""
                + ",\"errorCode\":\"" + code + "\",\"detail\":\"" + detail + "\"}");
    }
}
