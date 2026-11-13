package com.example.bankingapp;

import com.example.bankingapp.Google.*;
import com.example.bankingapp.Staff.LoginNamePolicy;
import com.example.bankingapp.Staff.StaffRepository;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(GoogleSignupTest.Config.class)
@TestPropertySource(properties = {
        "bank.security.google.enabled=true", "bank.security.google.client-id=test-client",
        "bank.security.google.client-secret=test-secret", "bank.security.configured-logins-enabled=false"
})
class GoogleSignupTest {
    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, GoogleOAuthConfiguration.class, GoogleSignupService.class,
            GoogleSignupController.class, LoginNamePolicy.class, MainExceptionHandler.class, CsrfController.class})
    static class Config {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean StaffRepository staff() { return mock(StaffRepository.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filters;
    @Autowired UserRepository users;
    @Autowired StaffRepository staff;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(users, staff);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });
    }

    static DefaultOidcUser identity(String subject, String email, boolean verified) {
        var token = new OidcIdToken("test-token", Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600),
                Map.of("iss", "https://accounts.google.com", "sub", subject, "email", email,
                        "email_verified", verified, "given_name", "<script>alert(1)</script>", "family_name", "Smith"));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_GOOGLE_PENDING")), token);
    }

    private MockHttpSession pending() {
        var principal = identity("google-subject", "customer@example.com", true);
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google"));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        return session;
    }

    private MockHttpServletRequestBuilder validForm() {
        return post("/signup/google").contentType("application/x-www-form-urlencoded")
                .param("username", "new.customer").param("firstName", "Jane").param("lastName", "Smith")
                .param("dateOfBirth", "1995-06-15");
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return request.session(session).header(token.getHeaderName(), token.getToken());
    }

    @Test
    void googleAuthorizationUsesStateAndNonceAndAddsLoginLink() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://accounts.google.com")))
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("nonce=")));
    }

    @Test
    void callbackWithoutMatchingAuthorizationRequestCannotSignIn() throws Exception {
        mvc.perform(get("/login/oauth2/code/google").param("code", "forged").param("state", "forged"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
        verifyNoInteractions(users);
    }

    @Test
    void formRequiresNamesAndDobAndEscapesGoogleClaims() throws Exception {
        mvc.perform(get("/signup/google").session(pending()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"firstName\" required")))
                .andExpect(content().string(containsString("name=\"lastName\" required")))
                .andExpect(content().string(containsString("name=\"dateOfBirth\" required")))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void signupStoresProfileAndGoogleSubjectAndCreatesOnlyCustomerSession() throws Exception {
        var session = pending();
        String oldId = session.getId();
        mvc.perform(withCsrf(validForm().param("role", "ADMIN").param("email", "attacker@example.com")
                        .param("googleSubject", "forged").param("creditScore", "850"), session))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/accounts/me"));
        var captured = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captured.capture());
        User saved = captured.getValue();
        assertEquals("Jane", saved.getFirstName());
        assertEquals("Smith", saved.getLastName());
        assertEquals(java.time.LocalDate.of(1995, 6, 15), saved.getDateOfBirth());
        assertEquals("customer@example.com", saved.getEmail());
        assertEquals("google-subject", saved.getGoogleSubject());
        assertNull(saved.getPassword());
        assertNull(saved.getCreditScore());
        assertNotEquals(oldId, session.getId());
        var security = (org.springframework.security.core.context.SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var principal = assertInstanceOf(CustomerPrincipal.class, security.getAuthentication().getPrincipal());
        assertEquals(42L, principal.getUserId());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), List.copyOf(principal.getAuthorities()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2999-01-01", "not-a-date"})
    void missingFutureOrInvalidDobDoesNotCreateAccount(String dob) throws Exception {
        mvc.perform(withCsrf(post("/signup/google").contentType("application/x-www-form-urlencoded")
                        .param("username", "new.customer").param("firstName", "Jane").param("lastName", "Smith")
                        .param("dateOfBirth", dob), pending()))
                .andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void underageGoogleSignupShowsTheAgeMessageAndDoesNotSave() throws Exception {
        mvc.perform(withCsrf(post("/signup/google").contentType("application/x-www-form-urlencoded")
                        .param("username", "new.customer").param("firstName", "Jane").param("lastName", "Smith")
                        .param("dateOfBirth", java.time.LocalDate.now().minusYears(18).plusDays(1).toString()), pending()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("dateOfBirth: You must be at least 18 years old to sign up.")));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void blankNamesDoNotCreateAccount() throws Exception {
        mvc.perform(withCsrf(post("/signup/google").contentType("application/x-www-form-urlencoded")
                        .param("username", "new.customer").param("firstName", " ").param("lastName", "")
                        .param("dateOfBirth", "1995-06-15"), pending()))
                .andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void matchingEmailIsNotAutomaticallyLinked() throws Exception {
        when(users.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);
        mvc.perform(withCsrf(validForm(), pending())).andExpect(status().isConflict())
                .andExpect(content().string(containsString("An account with this Google email already exists.")));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void pendingSignupCannotAccessBankingCommandsAndRequiresCsrf() throws Exception {
        var session = pending();
        for (String path : List.of("/accounts/me", "/users/me", "/accounts/user-1", "/users")) {
            mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(withCsrf(post("/staff"), session)).andExpect(status().isForbidden());
        mvc.perform(validForm().session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/signup/google").accept("application/json")).andExpect(status().isUnauthorized());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void unverifiedGoogleEmailIsRejected() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> GoogleSignupService.verifiedSubject(identity("subject", "customer@example.com", false)));
    }

    @Test
    void returningGoogleIdentityUsesSubjectEvenIfEmailChanges() {
        var existing = new User("old@example.com", null, "existing", "Jane", "Smith",
                java.time.LocalDate.of(1995, 6, 15), null);
        existing.linkGoogleSubject("stable-subject");
        when(users.findByGoogleSubject("stable-subject")).thenReturn(Optional.of(existing));
        var signup = context.getBean(GoogleSignupService.class);
        assertSame(existing, signup.findCustomer(identity("stable-subject", "new@example.com", true)).orElseThrow());
        verify(users, never()).existsByEmailIgnoreCase(any());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void googleCustomerCannotUseLocalPasswordAuthentication() {
        var existing = new User("customer@example.com", "unused-password", "existing", "Jane", "Smith",
                java.time.LocalDate.of(1995, 6, 15), null);
        existing.linkGoogleSubject("stable-subject");
        when(users.findByUsername("existing")).thenReturn(Optional.of(existing));
        var details = context.getBean(org.springframework.security.core.userdetails.UserDetailsService.class);
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> details.loadUserByUsername("existing"));
    }

    @Test
    void expiredTokenAndWrongIssuerAreRejected() {
        for (String issuer : List.of("https://accounts.google.com", "https://untrusted.example")) {
            var expires = issuer.contains("untrusted") ? Instant.now().plusSeconds(3600) : Instant.now().minusSeconds(30);
            var token = new OidcIdToken("test", Instant.now().minusSeconds(3600), expires,
                    Map.of("iss", issuer, "sub", "subject", "email", "customer@example.com", "email_verified", true));
            var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_GOOGLE_PENDING")), token);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> GoogleSignupService.verifiedSubject(principal));
        }
    }
}
