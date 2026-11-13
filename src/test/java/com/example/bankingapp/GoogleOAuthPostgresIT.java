package com.example.bankingapp;

import com.example.bankingapp.User.UserRepository;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP code exchange and RSA-signed OIDC tokens, with a local stand-in for Google. */
@SpringBootTest(properties = {
        "bank.security.google.enabled=true", "bank.security.google.client-id=test-client",
        "bank.security.google.client-secret=test-secret", "bank.security.configured-logins-enabled=false",
        "bank.security.login-attempts-per-minute=10000", "spring.flyway.baseline-on-migrate=false"
})
@ActiveProfiles("test")
@Import(GoogleOAuthPostgresIT.ProviderConfig.class)
class GoogleOAuthPostgresIT {
    static final IsolatedPostgres postgres = new IsolatedPostgres();
    static final OidcProvider provider = new OidcProvider();

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::url);
        properties.add("spring.datasource.username", () -> postgres.username);
        properties.add("spring.datasource.password", () -> postgres.password);
    }

    @TestConfiguration static class ProviderConfig {
        @Bean @Primary ClientRegistrationRepository testGoogleClient() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                    .clientId("test-client").clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email").authorizationUri(provider.base() + "/authorize")
                    .tokenUri(provider.base() + "/token").jwkSetUri(provider.base() + "/jwks")
                    .issuerUri("https://accounts.google.com").userInfoUri(provider.base() + "/userinfo")
                    .userNameAttributeName("sub").clientName("Google test provider").build());
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filters;
    @Autowired UserRepository users;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
        provider.subject = UUID.randomUUID().toString();
        provider.badNonce = false;
    }

    @AfterAll static void stopProvider() { provider.server.stop(0); }

    private MockHttpSession googleLogin(boolean success) throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        var query = Arrays.stream(URI.create(result.getResponse().getRedirectedUrl()).getRawQuery().split("&"))
                .map(part -> part.split("=", 2)).collect(Collectors.toMap(part -> part[0],
                        part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
        provider.nonce = query.get("nonce");
        assertNotNull(provider.nonce);
        var callback = mvc.perform(get("/login/oauth2/code/google").session(session)
                        .param("code", "test-code").param("state", query.get("state")))
                .andExpect(status().is3xxRedirection());
        if (!success) callback.andExpect(redirectedUrl("/login?error"));
        return session;
    }

    @Test void signedGoogleCodeExchangeProfileCompletionAndReturningLoginPersist() throws Exception {
        var session = googleLogin(true);
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.signupRequired").value(true))
                .andExpect(jsonPath("$.roles[0]").value("GOOGLE_PENDING"));
        mvc.perform(get("/accounts/me").session(session)).andExpect(status().isForbidden());
        var result = mvc.perform(get("/csrf").session(session)).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        String username = "googleuser";
        mvc.perform(post("/signup/google").session(session).header(token.getHeaderName(), token.getToken())
                        .contentType("application/x-www-form-urlencoded").param("username", username)
                        .param("firstName", "Jane").param("lastName", "Smith").param("dateOfBirth", "1990-04-03"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/accounts/me"));
        var saved = users.findByGoogleSubject(provider.subject).orElseThrow();
        assertEquals("Jane", saved.getFirstName());
        assertNull(saved.getPassword());
        mvc.perform(get("/auth/me").session(session)).andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        var returningSession = googleLogin(true);
        mvc.perform(get("/auth/me").session(returningSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(saved.getId()))
                .andExpect(jsonPath("$.signupRequired").value(false));
        assertEquals(saved.getId(), users.findByUsername(username).orElseThrow().getId());
    }

    @Test void signedTokenWithWrongNonceCannotAuthenticate() throws Exception {
        provider.badNonce = true;
        var session = googleLogin(false);
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertTrue(users.findByGoogleSubject(provider.subject).isEmpty());
    }

    static final class OidcProvider {
        final RSAKey key;
        final HttpServer server;
        volatile String subject;
        volatile String nonce;
        volatile boolean badNonce;

        OidcProvider() {
            try {
                key = new RSAKeyGenerator(2048).keyID("integration-test").generate();
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory()));
                server.createContext("/jwks", exchange -> respond(exchange, new JWKSet(key.toPublicJWK()).toString()));
                server.createContext("/userinfo", exchange -> respond(exchange,
                        "{\"sub\":\"" + subject + "\",\"email\":\"" + subject + "@example.test\",\"email_verified\":true}"));
                server.createContext("/token", exchange -> {
                    exchange.getRequestBody().readAllBytes();
                    try {
                        var claims = new JWTClaimsSet.Builder().issuer("https://accounts.google.com")
                                .subject(subject).audience("test-client").issueTime(new Date())
                                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                                .claim("nonce", badNonce ? "wrong-nonce" : nonce)
                                .claim("email", subject + "@example.test").claim("email_verified", true).build();
                        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(), claims);
                        jwt.sign(new RSASSASigner(key));
                        respond(exchange, "{\"access_token\":\"test-access-token\",\"token_type\":\"Bearer\","
                                + "\"expires_in\":300,\"scope\":\"openid profile email\",\"id_token\":\"" + jwt.serialize() + "\"}");
                    } catch (Exception ex) { throw new IllegalStateException(ex); }
                });
                server.start();
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        }

        String base() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

        private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        }
    }
}
