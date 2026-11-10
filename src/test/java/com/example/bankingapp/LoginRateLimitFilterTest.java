package com.example.bankingapp;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {
    @Test
    void limitsBasicAndFormAttemptsTogetherAndRecoversAfterWindow() throws Exception {
        var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        var filter = new LoginRateLimitFilter(2, clock);
        var calls = new AtomicInteger();
        var form = new MockHttpServletRequest("POST", "/login");
        form.setServletPath("/login");
        filter.doFilter(form, new MockHttpServletResponse(), (request, response) -> calls.incrementAndGet());
        var basic = new MockHttpServletRequest("GET", "/users");
        basic.addHeader("Authorization", "Basic invalid");
        filter.doFilter(basic, new MockHttpServletResponse(), (request, response) -> calls.incrementAndGet());
        var blocked = new MockHttpServletResponse();
        filter.doFilter(basic, blocked, (request, response) -> calls.incrementAndGet());
        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        assertEquals(2, calls.get());
        when(clock.millis()).thenReturn(60_000L);
        filter.doFilter(basic, new MockHttpServletResponse(), (request, response) -> calls.incrementAndGet());
        assertEquals(3, calls.get());
    }

    @Test
    void forwardedHeadersCannotBypassLimitAndSessionRequestsStillWork() throws Exception {
        var filter = new LoginRateLimitFilter(1, Clock.systemUTC());
        var request = new MockHttpServletRequest("GET", "/users");
        request.addHeader("Authorization", "Basic invalid");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        request.addHeader("X-Forwarded-For", "another-address");
        var blocked = new MockHttpServletResponse();
        filter.doFilter(request, blocked, (req, res) -> fail("Should be throttled"));
        assertEquals(429, blocked.getStatus());
        var calls = new AtomicInteger();
        filter.doFilter(new MockHttpServletRequest("GET", "/users"), new MockHttpServletResponse(),
                (req, res) -> calls.incrementAndGet());
        assertEquals(1, calls.get());
    }
}
