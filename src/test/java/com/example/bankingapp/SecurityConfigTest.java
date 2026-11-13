package com.example.bankingapp;

import com.example.bankingapp.Account.AccountController;
import com.example.bankingapp.Account.AccountService;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.User.DTOs.UserResponse;
import com.example.bankingapp.User.UserController;
import com.example.bankingapp.User.UserService;
import com.example.bankingapp.User.UserRepository;
import com.example.bankingapp.Staff.*;
import com.example.bankingapp.Auth.AuthController;
import com.example.bankingapp.Auth.SignupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.security.user.name=representative",
        "spring.security.user.password=test-password",
        "bank.security.admin.username=admin",
        "bank.security.admin.password=admin-password",
        "bank.security.login-attempts-per-minute=10000"
})
class SecurityConfigTest {
    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, CsrfController.class, UserController.class, AccountController.class,
            StaffController.class, StaffService.class, LoginNamePolicy.class, MainExceptionHandler.class, AuthController.class})
    static class TestConfig {
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean AccountService accountService() { return mock(AccountService.class); }
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean StaffRepository staffRepository() { return mock(StaffRepository.class); }
        @Bean SignupService signupService() { return mock(SignupService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilter;
    @Autowired UserDetailsService userDetailsService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserService userService;
    @Autowired AccountService accountService;
    @Autowired UserRepository userRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired StaffService staffService;
    private final Map<String, StaffAccount> storedStaff = new HashMap<>();
    MockMvc mvc;

    private static final String USER_JSON = """
            {"email":"customer@example.com","username":"customer","password":"customer-password",
             "dateOfBirth":"1990-01-01","firstName":"Test","lastName":"Customer","creditScore":700}
            """;

    @BeforeEach
    void setUp() {
        accountService = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(AccountService.class));
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        reset(userRepository);
        reset(staffRepository);
        storedStaff.clear();
        when(staffRepository.findByUsername(any())).thenAnswer(invocation -> Optional.ofNullable(storedStaff.get(invocation.getArgument(0))));
        when(staffRepository.existsByUsername(any())).thenAnswer(invocation -> storedStaff.containsKey(invocation.getArgument(0)));
        when(staffRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            StaffAccount account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", (long) storedStaff.size() + 1);
            storedStaff.put(account.getUsername(), account);
            return account;
        });
        var customer = mock(com.example.bankingapp.User.User.class);
        when(customer.getId()).thenReturn(1L);
        when(customer.getUsername()).thenReturn("customer");
        when(customer.getPassword()).thenReturn(passwordEncoder.encode("test-password"));
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        reset(userService, accountService);
        var user = new UserResponse(1L, "customer", "customer@example.com", "Test", "Customer", 700);
        when(userService.createUser(any())).thenReturn(user);
        when(userService.getUserById(1L)).thenReturn(user);
        when(userService.getAllUsers()).thenReturn(List.of(user));
        when(accountService.createAccount(any())).thenReturn(new AccountResponse(1L, 1L, null, null, null, null));
        when(accountService.getAccounts(1L)).thenReturn(List.of());
    }

    @Test
    void configuredLoginHasRepresentativeAuthorityAndEncodedPassword() {
        var user = userDetailsService.loadUserByUsername("representative");
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BANK_REPRESENTATIVE")));
        assertNotEquals("test-password", user.getPassword());
        assertTrue(passwordEncoder.matches("test-password", user.getPassword()));
    }

    @ParameterizedTest
    @CsvSource({"GET,/users", "GET,/users/1", "POST,/users", "PUT,/users/1",
            "GET,/accounts/user-1", "POST,/accounts", "PUT,/accounts"})
    void representativeCanUseEveryExistingCommand(String method, String path) throws Exception {
        mvc.perform(withToken(command(method, path)).header("Authorization", basic("representative", "test-password")))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @CsvSource({"GET,/users", "GET,/users/1", "POST,/users", "PUT,/users/1",
            "GET,/accounts/user-2", "POST,/accounts", "PUT,/accounts"})
    void customerCannotUseStaffCommands(String method, String path) throws Exception {
        mvc.perform(withToken(command(method, path)).header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService, accountService);
    }

    @ParameterizedTest
    @CsvSource({"GET,/users", "GET,/users/1", "POST,/users", "PUT,/users/1",
            "GET,/accounts/user-1", "GET,/accounts/user-2", "POST,/accounts", "PUT,/accounts"})
    void adminCanUseEveryCommand(String method, String path) throws Exception {
        mvc.perform(withToken(command(method, path)).header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void customerCanOnlyReadOwnAccounts() throws Exception {
        mvc.perform(get("/accounts/user-1").header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isOk());
        verify(accountService).getAccounts(1L);
        mvc.perform(get("/accounts/user-2").header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isForbidden());
        verify(accountService, never()).getAccounts(2L);
        mvc.perform(get("/csrf").header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isOk());
        verifyNoInteractions(userService);
    }

    @Test
    void anonymousCannotReadAccounts() throws Exception {
        mvc.perform(get("/accounts/user-1").accept("application/json"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accountService);
    }

    @Test
    void anonymousAndIncorrectCredentialsAreRejected() throws Exception {
        mvc.perform(get("/users").accept("application/json")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users").header("Authorization", basic("representative", "wrong")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService, accountService);
    }

    @Test
    void writesRequireCsrfTokenEvenForRepresentative() throws Exception {
        mvc.perform(command("POST", "/users").header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService, accountService);
    }

    @Test
    void apiClientCanFetchTokenAndCreateCustomerWithSameSession() throws Exception {
        var result = mvc.perform(get("/csrf").header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN")).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(command("POST", "/users")
                        .session((MockHttpSession) result.getRequest().getSession(false))
                        .header(token.getHeaderName(), token.getToken())
                        .header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isCreated());
        verify(userService).createUser(any());
    }

    private MockHttpServletRequestBuilder command(String method, String path) {
        String body = path.startsWith("/users") ? USER_JSON
                : "{\"userId\":1,\"accountNumber\":\"1000000001\",\"accountType\":\"CHECKING\"}";
        return request(HttpMethod.valueOf(method), path).param("id", "1")
                .contentType("application/json").accept("application/json").content(body);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "BANK_REPRESENTATIVE"})
    void adminCreatesStoredStaffWhoCanThenLogIn(String role) throws Exception {
        mvc.perform(withToken(staffRequest("new.staff", role))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new.staff"))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.password").doesNotExist());
        var saved = storedStaff.get("new.staff");
        assertEquals("admin", saved.getCreatedBy());
        assertNotNull(saved.getCreatedAt());
        assertNotEquals("new-staff-password", saved.getPassword());
        assertTrue(passwordEncoder.matches("new-staff-password", saved.getPassword()));
        mvc.perform(get("/users").header("Authorization", basic("new.staff", "new-staff-password")))
                .andExpect(status().isOk());
        mvc.perform(withToken(staffRequest("another.staff", "ADMIN"))
                        .header("Authorization", basic("new.staff", "new-staff-password")))
                .andExpect(role.equals("ADMIN") ? status().isCreated() : status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"representative,test-password", "customer,test-password"})
    void nonAdminsCannotCreateStaff(String username, String password) throws Exception {
        mvc.perform(withToken(staffRequest("new.staff", "ADMIN"))
                        .header("Authorization", basic(username, password)))
                .andExpect(status().isForbidden());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void anonymousAndMissingCsrfCannotCreateStaff() throws Exception {
        mvc.perform(withToken(staffRequest("new.staff", "ADMIN")))
                .andExpect(status().isUnauthorized());
        mvc.perform(staffRequest("new.staff", "ADMIN").header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isForbidden());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"admin", "ADMIN", "representative", "customer"})
    void existingAndConfiguredNamesCannotBeReused(String username) throws Exception {
        mvc.perform(withToken(staffRequest(username, "ADMIN"))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isConflict());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateStoredStaffIsRejected() throws Exception {
        mvc.perform(withToken(staffRequest("new.staff", "ADMIN"))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isCreated());
        mvc.perform(withToken(staffRequest("new.staff", "BANK_REPRESENTATIVE"))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isConflict());
        assertEquals(StaffRole.ADMIN, storedStaff.get("new.staff").getRole());
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER", "SUPERADMIN"})
    void unsupportedStaffRolesAreRejected(String role) throws Exception {
        mvc.perform(withToken(staffRequest("new.staff", role))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isBadRequest());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidStaffCredentialsAreRejected() throws Exception {
        mvc.perform(withToken(post("/staff").contentType("application/json")
                        .content("{\"username\":\"x\",\"password\":\"short\",\"role\":\"ADMIN\"}"))
                        .header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isBadRequest());
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void serviceCannotBeCalledWithoutAdminAndUnknownEndpointsAreDenied() throws Exception {
        assertThrows(org.springframework.security.core.AuthenticationException.class,
                () -> staffService.create(new StaffRequest("blocked.staff", "long-enough-password", StaffRole.ADMIN)));
        verify(staffRepository, never()).saveAndFlush(any());
        mvc.perform(get("/new-unconfigured-endpoint").header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder staffRequest(String username, String role) {
        return post("/staff").contentType("application/json").accept("application/json")
                .content("{\"username\":\"" + username + "\",\"password\":\"new-staff-password\",\"role\":\"" + role + "\"}");
    }

    @Test
    void browserLoginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        var loginPage = mvc.perform(get("/login")).andReturn();
        var session = (MockHttpSession) loginPage.getRequest().getSession(false);
        String oldId = session.getId();
        var token = (CsrfToken) loginPage.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(session).header(token.getHeaderName(), token.getToken())
                        .param("username", "customer").param("password", "test-password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/accounts/me"));
        assertNotEquals(oldId, session.getId());
        mvc.perform(get("/accounts/user-1").session(session)).andExpect(status().isOk());
        var csrfResult = mvc.perform(get("/csrf").session(session)).andReturn();
        var logoutToken = (CsrfToken) csrfResult.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/logout").session(session).header(logoutToken.getHeaderName(), logoutToken.getToken()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }

    @Test
    void productionCanDisableConfiguredCredentialsWhileKeepingDatabaseLogins() {
        var details = new SecurityConfig().userDetailsService("representative", "test-password",
                "admin", "admin-password", false, passwordEncoder, userRepository, staffRepository);
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> details.loadUserByUsername("admin"));
        storedStaff.put("stored.admin", new StaffAccount("stored.admin", passwordEncoder.encode("stored-password"), StaffRole.ADMIN));
        assertTrue(details.loadUserByUsername("stored.admin").getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void myAccountsReturnsBankingDetailsAndIgnoresSuppliedOwnerId() throws Exception {
        when(accountService.getAccounts(1L)).thenReturn(List.of(
                new AccountResponse(10L, 1L, com.example.bankingapp.Account.AccountType.CHECKING,
                        "1000000001", "91000019", new java.math.BigDecimal("1250.75")),
                new AccountResponse(11L, 1L, com.example.bankingapp.Account.AccountType.SAVINGS,
                        "1000000002", "91000019", new java.math.BigDecimal("5000.00"))));
        mvc.perform(get("/accounts/me").param("userId", "2").param("id", "2")
                        .header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value("1000000001"))
                .andExpect(jsonPath("$[0].routingNumber").value("91000019"))
                .andExpect(jsonPath("$[0].availableBalance").value(1250.75))
                .andExpect(jsonPath("$[1].accountType").value("SAVINGS"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        verify(accountService).getAccounts(1L);
        verify(accountService, never()).getAccounts(2L);
        verifyNoInteractions(userService);
    }

    @Test
    void myAccountsUsesEachCustomersIdentityAndReturnsEmptyListWhenNoAccounts() throws Exception {
        var second = mock(com.example.bankingapp.User.User.class);
        when(second.getId()).thenReturn(2L);
        when(second.getUsername()).thenReturn("second.customer");
        when(second.getPassword()).thenReturn(passwordEncoder.encode("second-password"));
        when(userRepository.findByUsername("second.customer")).thenReturn(Optional.of(second));
        when(accountService.getAccounts(2L)).thenReturn(List.of());
        mvc.perform(get("/accounts/me").header("Authorization", basic("second.customer", "second-password")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(accountService).getAccounts(2L);
        verify(accountService, never()).getAccounts(1L);
    }

    @Test
    void myAccountsRequiresCustomerLogin() throws Exception {
        mvc.perform(get("/accounts/me").accept("application/json")).andExpect(status().isUnauthorized());
        mvc.perform(get("/accounts/me").header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/accounts/me").header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accountService);
    }

    @Test
    void customerProfileUsesLoginIdentityAndNeverReturnsPassword() throws Exception {
        mvc.perform(get("/users/me").param("id", "2")
                        .header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        verify(userService).getUserById(1L);
        verify(userService, never()).getUserById(2L);
    }

    @Test
    void customerCanReadSingleAccountUsingAuthenticatedOwner() throws Exception {
        when(accountService.getCustomerAccount(10L, 1L)).thenReturn(new AccountResponse(10L, 1L,
                com.example.bankingapp.Account.AccountType.CHECKING, "1000000001", "91000019", new java.math.BigDecimal("1250.75")));
        mvc.perform(get("/accounts/me/10").param("userId", "2")
                        .header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.availableBalance").value(1250.75));
        verify(accountService).getCustomerAccount(10L, 1L);
        verify(accountService, never()).getCustomerAccount(10L, 2L);
    }

    @Test
    void unownedOrMissingAccountReturnsNotFound() throws Exception {
        when(accountService.getCustomerAccount(20L, 1L))
                .thenThrow(new com.example.bankingapp.Account.AccountNotFoundException(20L));
        mvc.perform(get("/accounts/me/20").header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "not-a-number", "999999999999999999999999"})
    void invalidCustomerAccountIdReturnsBadRequest(String id) throws Exception {
        mvc.perform(get("/accounts/me/" + id).header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @CsvSource({"/users/me", "/accounts/me/10"})
    void customerEndpointsRejectAnonymousAndStaff(String path) throws Exception {
        mvc.perform(get(path).accept("application/json")).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", basic("admin", "admin-password")))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(put(path)).header("Authorization", basic("customer", "test-password")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accountService, userService);
    }

    @Test
    void customerAccountServiceRejectsForgedOwnerEvenOutsideHttp() {
        var secured = context.getBean(AccountService.class);
        assertThrows(org.springframework.security.core.AuthenticationException.class,
                () -> secured.getCustomerAccount(10L, 1L));
        var principal = new CustomerPrincipal(1L, "customer", "unused-hash");
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> secured.getCustomerAccount(10L, 2L));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short!", "JamesSmith", "James123!Lane", "JamesaBc!Lane"})
    void invalidSignupAndReplacementPasswordsReturnRequirements(String password) throws Exception {
        String json = USER_JSON.replace("customer-password", password);
        for (String method : List.of("POST", "PUT")) {
            String path = method.equals("POST") ? "/users" : "/users/1";
            mvc.perform(withToken(request(HttpMethod.valueOf(method), path).contentType("application/json").content(json))
                            .header("Authorization", basic("representative", "test-password")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", org.hamcrest.Matchers.containsString("at least 10 characters")))
                    .andExpect(jsonPath("$.errors[0]", org.hamcrest.Matchers.containsString("special character")));
        }
        verifyNoInteractions(userService);
    }

    @Test
    void tenCharacterSignupPasswordWithSpecialCharacterIsAccepted() throws Exception {
        mvc.perform(withToken(post("/users").contentType("application/json")
                        .content(USER_JSON.replace("customer-password", "James!Lane")))
                        .header("Authorization", basic("representative", "test-password")))
                .andExpect(status().isCreated());
        verify(userService).createUser(any());
    }

    @Test
    void anonymousFrontendCanBootstrapCsrfAndGetsJsonAuthenticationErrors() throws Exception {
        mvc.perform(get("/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(withToken(post("/login").accept("application/json")
                .param("username", "missing").param("password", "wrong")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void frontendLoginReturnsNoRedirectAndExposesTheRole() throws Exception {
        var request = withToken(post("/login").accept("application/json")
                .param("username", "customer").param("password", "test-password"));
        var result = mvc.perform(request).andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location")).andReturn();
        mvc.perform(get("/auth/me").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request) throws Exception {
        var result = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        String value = token.getToken();
        return request.session((MockHttpSession) result.getRequest().getSession(false))
                .header(token.getHeaderName(), value);
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
