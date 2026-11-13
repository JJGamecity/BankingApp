package com.example.bankingapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.example.bankingapp.User.UserRepository;
import com.example.bankingapp.Staff.StaffRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import com.example.bankingapp.Google.GoogleOAuthConfiguration;
import com.example.bankingapp.Auth.SecurityResponses;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String BANK_REPRESENTATIVE = "BANK_REPRESENTATIVE";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${bank.security.login-attempts-per-minute:30}") int loginLimit,
            @Value("${bank.security.frontend-origins:http://localhost:5173,http://127.0.0.1:5173}") String frontendOrigins,
            ObjectProvider<GoogleOAuthConfiguration> google) throws Exception {
        var googleConfig = google.getIfAvailable();
        if (googleConfig != null) googleConfig.configure(http);
        var cors = new CorsConfiguration();
        var origins = Arrays.stream(frontendOrigins.split(",")).map(String::strip)
                .filter(origin -> !origin.isBlank()).toList();
        if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("Frontend origins must be explicit origins, without wildcards");
        }
        cors.setAllowedOrigins(origins);
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Authorization", "Accept"));
        cors.setExposedHeaders(List.of("Retry-After", "Location"));
        var corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", cors);
        return http
                .cors(config -> config.configurationSource(corsSource))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/authorization/google", "/login/oauth2/code/google").permitAll()
                        .requestMatchers("/signup/google").hasRole("GOOGLE_PENDING")
                        .requestMatchers("/staff", "/staff/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/me", "/accounts/me", "/accounts/me/{accountId}").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.GET, "/accounts/user-{id}")
                        .access((authentication, context) -> {
                            var auth = authentication.get();
                            boolean staff = auth.getAuthorities().stream().anyMatch(authority ->
                                    authority.getAuthority().equals("ROLE_" + ADMIN)
                                            || authority.getAuthority().equals("ROLE_" + BANK_REPRESENTATIVE));
                            boolean owner = auth.getPrincipal() instanceof CustomerPrincipal customer
                                    && customer.getUserId().toString().equals(context.getVariables().get("id"));
                            return new AuthorizationDecision(auth.isAuthenticated() && (staff || owner));
                        })
                        .requestMatchers("/users", "/users/**", "/accounts", "/accounts/**")
                        .hasAnyRole(BANK_REPRESENTATIVE, ADMIN)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole(ADMIN)
                        .anyRequest().denyAll())
                .addFilterBefore(new LoginRateLimitFilter(loginLimit, Clock.systemUTC()), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor((request, response, ex) -> SecurityResponses.error(response, 401,
                                "AUTHENTICATION_REQUIRED", "Sign in to continue. Your session may have expired."), request -> true)
                        .accessDeniedHandler((request, response, ex) -> SecurityResponses.error(response, 403,
                                ex instanceof CsrfException ? "INVALID_CSRF_TOKEN" : "ACCESS_DENIED",
                                ex instanceof CsrfException ? "Refresh the CSRF token and try again." : "Access denied.")))
                .logout(logout -> logout.invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, ex) ->
                        SecurityResponses.error(response, 401, "INVALID_CREDENTIALS", "Invalid username or password.")))
                .formLogin(form -> form.permitAll().successHandler((request, response, authentication) -> {
                    if (SecurityResponses.wantsJson(request)) {
                        response.setStatus(204);
                        return;
                    }
                    String destination = authentication.getPrincipal() instanceof CustomerPrincipal customer
                            ? "/accounts/me" : "/users";
                    response.sendRedirect(request.getContextPath() + destination);
                }).failureHandler((request, response, ex) -> {
                    if (SecurityResponses.wantsJson(request)) {
                        SecurityResponses.error(response, 401, "INVALID_CREDENTIALS", "Invalid username or password.");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login?error");
                    }
                }))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    UserDetailsService userDetailsService(
            @Value("${spring.security.user.name:}") String username,
            @Value("${spring.security.user.password:}") String password,
            @Value("${bank.security.admin.username:}") String adminUsername,
            @Value("${bank.security.admin.password:}") String adminPassword,
            @Value("${bank.security.configured-logins-enabled:true}") boolean configuredLoginsEnabled,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            StaffRepository staffRepository
    ) {
        // Staff authentication is separate from customer records in the users table.
        var staff = new InMemoryUserDetailsManager();
        if (configuredLoginsEnabled && username.isBlank() != password.isBlank()) {
            throw new IllegalArgumentException("Configure both representative username and password");
        }
        if (configuredLoginsEnabled && !username.isBlank()) {
            staff.createUser(User.withUsername(username).password(passwordEncoder.encode(password))
                    .roles(BANK_REPRESENTATIVE).build());
        }
        if (configuredLoginsEnabled && adminUsername.isBlank() != adminPassword.isBlank()) {
            throw new IllegalArgumentException("Configure both admin username and password");
        }
        if (configuredLoginsEnabled && !adminUsername.isBlank()) {
            if (adminUsername.equalsIgnoreCase(username)) {
                throw new IllegalArgumentException("Admin and representative usernames must be different");
            }
            staff.createUser(User.withUsername(adminUsername)
                    .password(passwordEncoder.encode(adminPassword)).roles(ADMIN).build());
        }
        return login -> {
            if (staff.userExists(login)) {
                return staff.loadUserByUsername(login);
            }
            var storedStaff = staffRepository.findByUsername(login);
            if (storedStaff.isPresent()) {
                var account = storedStaff.get();
                return User.withUsername(account.getUsername()).password(account.getPassword())
                        .roles(account.getRole().name()).build();
            }
            var customer = userRepository.findByUsername(login)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
            if (customer.getGoogleSubject() != null || customer.getPassword() == null) {
                throw new UsernameNotFoundException("Use Google sign-in for this account");
            }
            return new CustomerPrincipal(customer.getId(), customer.getUsername(), customer.getPassword());
        };
    }
}
