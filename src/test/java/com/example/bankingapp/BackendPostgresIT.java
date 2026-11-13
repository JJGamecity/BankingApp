package com.example.bankingapp;

import com.example.bankingapp.Account.Account;
import com.example.bankingapp.Account.AccountRepository;
import com.example.bankingapp.Account.AccountType;
import com.example.bankingapp.Staff.StaffAccount;
import com.example.bankingapp.Staff.StaffRepository;
import com.example.bankingapp.Staff.StaffRole;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.user.name=", "spring.security.user.password=",
        "bank.security.admin.username=bootstrapadmin", "bank.security.admin.password=Test!Admin908",
        "bank.security.google.enabled=false", "bank.security.login-attempts-per-minute=10000",
        "spring.flyway.baseline-on-migrate=false", "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class BackendPostgresIT {
    static final IsolatedPostgres postgres = new IsolatedPostgres();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::url);
        properties.add("spring.datasource.username", () -> postgres.username);
        properties.add("spring.datasource.password", () -> postgres.password);
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserRepository users;
    @Autowired StaffRepository staff;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    private String unique() { return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12); }

    private String signupJson(String username) {
        return """
                {"username":"%s","email":"%s@example.test","password":"James!90758",
                 "firstName":"James","lastName":"Smith","dateOfBirth":"1995-06-15"}
                """.formatted(username, username);
    }

    private MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return request.session(session).header(token.getHeaderName(), token.getToken());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        var session = new MockHttpSession();
        String oldId = session.getId();
        mvc.perform(csrf(post("/login").accept("application/json").param("username", username)
                        .param("password", password), session)).andExpect(status().isNoContent());
        assertNotEquals(oldId, session.getId());
        return session;
    }

    private User customer(String username) {
        return users.saveAndFlush(new User(username + "@example.test", passwords.encode("James!90758"),
                username, "James", "Smith", LocalDate.of(1995, 6, 15), null));
    }

    @Test void publicSignupPersistsOnlyCustomerAndLoginLogoutWork() throws Exception {
        String username = unique();
        var anonymous = new MockHttpSession();
        String payload = signupJson(username).replace("\"firstName\"", "\"role\":\"ADMIN\",\"creditScore\":850,\"firstName\"");
        mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(payload), anonymous))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.creditScore").isEmpty());
        var saved = users.findByUsername(username).orElseThrow();
        assertTrue(passwords.matches("James!90758", saved.getPassword()));
        assertNotEquals("James!90758", saved.getPassword());
        assertFalse(staff.existsByUsername(username));
        mvc.perform(get("/auth/me").session(anonymous)).andExpect(status().isUnauthorized());
        var session = login(username, "James!90758");
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(saved.getId()))
                .andExpect(jsonPath("$.signupRequired").value(false));
        mvc.perform(get("/users").session(session)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        mvc.perform(csrf(post("/logout"), session)).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    @Test void signupRequiresCsrfValidPasswordNamesAndPastDob() throws Exception {
        String username = unique();
        mvc.perform(post("/auth/signup").contentType("application/json").content(signupJson(username)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("INVALID_CSRF_TOKEN"));
        for (String invalid : List.of(signupJson(username).replace("James!90758", "abc123"),
                signupJson(username).replace("1995-06-15", "2995-06-15"),
                signupJson(username).replace("\"James\"", "\"\""))) {
            mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(invalid), new MockHttpSession()))
                    .andExpect(status().isBadRequest());
        }
        assertTrue(users.findByUsername(username).isEmpty());
    }

    @Test void duplicateEmailAndReservedStaffNamesAreRejected() throws Exception {
        String username = unique();
        customer(username);
        for (String payload : List.of(signupJson(username), signupJson("bootstrapadmin"),
                signupJson(unique()).replaceAll("\"email\":\"[^\"]+\"", "\"email\":\"" + username.toUpperCase() + "@EXAMPLE.TEST\""))) {
            mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(payload), new MockHttpSession()))
                    .andExpect(status().isConflict());
        }
    }

    @Test void badLoginAndExpiredSessionsHaveJsonErrors() throws Exception {
        mvc.perform(csrf(post("/login").accept("application/json").param("username", "missing")
                .param("password", "wrong"), new MockHttpSession())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        mvc.perform(get("/auth/me").cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "expired")))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test void signupAgeAndEmailConflictsReturnClearMessagesAndDoNotSave() throws Exception {
        String username = unique();
        var underage = signupJson(username).replace("1995-06-15", LocalDate.now().minusYears(18).plusDays(1).toString());
        mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(underage), new MockHttpSession()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasItem(
                        "dateOfBirth: You must be at least 18 years old to sign up.")));
        assertTrue(users.findByUsername(username).isEmpty());
        var adult = signupJson(username).replace("1995-06-15", LocalDate.now().minusYears(18).toString());
        mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(adult), new MockHttpSession()))
                .andExpect(status().isCreated());
        String second = unique();
        var duplicateEmail = signupJson(second).replace(second + "@example.test", username + "@example.test");
        mvc.perform(csrf(post("/auth/signup").contentType("application/json").content(duplicateEmail), new MockHttpSession()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("SIGNUP_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("An account with this email address already exists. Sign in or use another email address."));
        assertTrue(users.findByUsername(second).isEmpty());
    }

    @Test void everyStaffRoleHasSessionInformation() throws Exception {
        for (var role : StaffRole.values()) {
            String username = unique();
            staff.saveAndFlush(new StaffAccount(username, passwords.encode("James!90758"), role));
            mvc.perform(get("/auth/me").session(login(username, "James!90758")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value(role.name()))
                    .andExpect(jsonPath("$.customerId").isEmpty());
        }
    }

    @Test void corsAllowsOnlyConfiguredFrontend() throws Exception {
        mvc.perform(options("/auth/signup").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/auth/signup").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void ownAccountsAreReturnedAndOtherCustomersAccountsStayHidden() throws Exception {
        var owner = customer(unique());
        var stranger = customer(unique());
        var account = accounts.saveAndFlush(new Account(owner, AccountType.CHECKING, unique(), "91000019"));
        mvc.perform(get("/accounts/me/" + account.getId()).session(login(owner.getUsername(), "James!90758")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountNumber").value(account.getAccountNumber()));
        mvc.perform(get("/accounts/me/" + account.getId()).session(login(stranger.getUsername(), "James!90758")))
                .andExpect(status().isNotFound());
    }

    @Test void profileEditsActuallyPersistAllEditableFields() throws Exception {
        var customer = customer(unique());
        String renamed = unique();
        var payload = signupJson(renamed).replace("\"James\"", "\"Updated\"")
                .replace("\"Smith\"", "\"Surname\"").replace("1995-06-15", "1988-02-03")
                .replace("}", ",\"creditScore\":740}");
        mvc.perform(csrf(put("/users/" + customer.getId()).contentType("application/json").content(payload),
                login("bootstrapadmin", "Test!Admin908"))).andExpect(status().isNoContent());
        var updated = users.findById(customer.getId()).orElseThrow();
        assertEquals("Updated", updated.getFirstName());
        assertEquals("Surname", updated.getLastName());
        assertEquals(740, updated.getCreditScore());
        assertEquals(LocalDate.of(1988, 2, 3), updated.getDateOfBirth());
        assertEquals(renamed, updated.getUsername());
        assertTrue(passwords.matches("James!90758", updated.getPassword()));
        assertEquals(0, jdbc.queryForObject("select count(*) from login_names where username=?", Integer.class, customer.getUsername()));
    }

    @Test void databaseConstraintsEnforceEmailAndCrossRoleUsernameUniqueness() {
        var user = customer(unique());
        assertThrows(DataIntegrityViolationException.class, () -> staff.saveAndFlush(
                new StaffAccount(user.getUsername(), passwords.encode("James!90758"), StaffRole.ADMIN)));
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(new User(user.getEmail().toUpperCase(),
                "hash", unique(), "Other", "Person", LocalDate.of(1990, 1, 1), null)));
        assertEquals(0, flyway.info().pending().length);
        flyway.validate();
    }

    @Test void concurrentCustomerAndStaffCreationCannotClaimTheSameLogin() throws Exception {
        String username = unique();
        var barrier = new CyclicBarrier(2);
        Callable<Boolean> customerWrite = () -> {
            barrier.await();
            try { customer(username); return true; } catch (DataIntegrityViolationException ex) { return false; }
        };
        Callable<Boolean> staffWrite = () -> {
            barrier.await();
            try { staff.saveAndFlush(new StaffAccount(username, "hash", StaffRole.ADMIN)); return true; }
            catch (DataIntegrityViolationException ex) { return false; }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(customerWrite, staffWrite));
            assertNotEquals(results.get(0).get(), results.get(1).get());
        }
    }

    @Test void migrationsAdoptLegacyTablesWithoutLosingData() {
        jdbc.execute("create schema legacy_test");
        jdbc.execute("create table legacy_test.users (id bigint generated by default as identity primary key, "
                + "email varchar(255) unique, username varchar(255) unique, password varchar(255), first_name varchar(255), "
                + "last_name varchar(255), dob date, credit_score integer)");
        jdbc.update("insert into legacy_test.users (username,email,first_name) values ('legacy','legacy@example.test','Preserved')");
        var migration = Flyway.configure().dataSource(postgres.url(), postgres.username, postgres.password)
                .defaultSchema("legacy_test").baselineOnMigrate(true).baselineVersion("0").load();
        migration.migrate();
        migration.migrate();
        migration.validate();
        assertEquals("Preserved", jdbc.queryForObject("select first_name from legacy_test.users where username='legacy'", String.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from legacy_test.login_names", Integer.class));
        jdbc.update("insert into legacy_test.users (username,email) values ('legacy_new','legacy_new@example.test')");
        assertEquals(2, jdbc.queryForObject("select count(*) from legacy_test.login_names", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from public.login_names where username='legacy_new'", Integer.class));
    }

    @Test @Order(999) void committedCustomerSurvivesDatabaseRestart() throws Exception {
        var saved = customer(unique());
        postgres.restart();
        try (var connection = DriverManager.getConnection(postgres.url(), postgres.username, postgres.password);
             var statement = connection.prepareStatement("select username from users where id=?")) {
            statement.setLong(1, saved.getId());
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(saved.getUsername(), result.getString(1));
            }
        }
    }
}

