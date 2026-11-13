package com.example.bankingapp.Google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "bank.security.google.enabled", havingValue = "true")
public class GoogleOAuthConfiguration {
    private final GoogleSignupService signup;

    public GoogleOAuthConfiguration(GoogleSignupService signup) { this.signup = signup; }

    @Bean
    ClientRegistrationRepository googleClients(
            @Value("${bank.security.google.client-id}") String clientId,
            @Value("${bank.security.google.client-secret}") String clientSecret) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Google client ID and secret must be configured before enabling Google login");
        }
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId).clientSecret(clientSecret).scope("openid", "profile", "email").build());
    }

    public void configure(HttpSecurity http) throws Exception {
        var delegate = new OidcUserService();
        http.oauth2Login(oauth -> oauth.userInfoEndpoint(info -> info.oidcUserService(request -> {
            // Spring validates signature, issuer, audience, nonce and the authorization-code flow.
            OidcUser user = delegate.loadUser(request);
            try {
                GoogleSignupService.verifiedSubject(user);
            } catch (AccessDeniedException ex) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_identity"));
            }
            return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_GOOGLE_PENDING")),
                    user.getIdToken(), user.getUserInfo());
        })).successHandler((request, response, authentication) -> {
            var existing = signup.findCustomer((OidcUser) authentication.getPrincipal());
            if (existing.isPresent()) {
                GoogleCustomerSession.signIn(existing.get(), request, response);
                response.sendRedirect(request.getContextPath() + "/accounts/me");
            } else {
                response.sendRedirect(request.getContextPath() + "/signup/google");
            }
        }));
    }
}
