package com.example.bankingapp;

import com.example.bankingapp.Account.AccountController;
import com.example.bankingapp.Account.AccountService;
import com.example.bankingapp.User.UserController;
import com.example.bankingapp.User.UserService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MainExceptionHandlerTest {
    private final UserService users = mock(UserService.class);
    private final AccountService accounts = mock(AccountService.class);
    private final FailureController failures = new FailureController();
    private MockMvc mvc;
    private static final String ACCOUNT = "{\"userId\":1,\"accountNumber\":\"1001\",\"accountType\":\"CHECKING\"}";

    @RestController
    static class FailureController {
        RuntimeException failure;
        @GetMapping("/failure") public String fail() { throw failure; }
        @NotNull @GetMapping("/invalid-response") public String invalidResponse() { return null; }
    }

    static class InvalidValue {
        @NotBlank @Size(min = 5) String value = "";
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(users), new AccountController(accounts), failures)
                .setControllerAdvice(new MainExceptionHandler()).build();
        when(users.getAllUsers()).thenReturn(List.of());
    }

    @Test
    void frameworkHttpErrorsKeepTheirStatusesAndAllowHeader() throws Exception {
        mvc.perform(patch("/users")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/users").contentType("text/plain").content("private-request-value"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string(not(containsString("private-request-value"))));
        mvc.perform(get("/users").accept("application/xml")).andExpect(status().isNotAcceptable());
        mvc.perform(get("/unmapped-resource")).andExpect(status().isNotFound());
    }

    @Test
    void invalidJsonAndMissingBodyReturn400() throws Exception {
        mvc.perform(post("/users").contentType("application/json").content("{bad-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mvc.perform(post("/users").contentType("application/json"))
                .andExpect(status().isBadRequest());
        verify(users, never()).createUser(any());
    }

    @Test
    void missingAndMalformedIdsReturnSafe400Responses() throws Exception {
        mvc.perform(put("/accounts").contentType("application/json").content(ACCOUNT))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/users/private-request-value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", not(containsString("private-request-value"))))
                .andExpect(content().string(not(containsString("java.lang"))));
        mvc.perform(put("/accounts").param("id", "0").contentType("application/json").content(ACCOUNT))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }

    @Test
    void incompleteAccountRequestIsRejectedBeforeDatabaseAccess() throws Exception {
        mvc.perform(post("/accounts").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(post("/accounts").contentType("application/json").content(ACCOUNT.replace("\"userId\":1", "\"userId\":0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }

    @Test
    void databaseFailuresAreSanitizedAndClassified() throws Exception {
        failures.failure = new org.springframework.web.context.request.async.AsyncRequestTimeoutException();
        mvc.perform(get("/failure")).andExpect(status().isServiceUnavailable());
        for (RuntimeException ex : List.of(new DataAccessResourceFailureException("sensitive-db-detail"),
                new QueryTimeoutException("sensitive-db-detail"), new CannotCreateTransactionException("sensitive-db-detail"))) {
            failures.failure = ex;
            mvc.perform(get("/failure")).andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(not(containsString("sensitive-db-detail"))));
        }
        for (RuntimeException ex : List.of(new DataIntegrityViolationException("sensitive-db-detail"),
                new OptimisticLockingFailureException("sensitive-db-detail"))) {
            failures.failure = ex;
            mvc.perform(get("/failure")).andExpect(status().isConflict())
                    .andExpect(content().string(not(containsString("sensitive-db-detail"))));
        }
    }

    @Test
    void multipleViolationsOnOneFieldDoNotCrashHandler() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            failures.failure = new ConstraintViolationException(factory.getValidator().validate(new InvalidValue()));
            mvc.perform(get("/failure")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(2));
        }
    }

    @Test
    void securityAndUnexpectedExceptionsReturnSafeResponses() throws Exception {
        failures.failure = new BadCredentialsException("sensitive-auth-detail");
        mvc.perform(get("/failure")).andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("sensitive-auth-detail"))));
        failures.failure = new AccessDeniedException("sensitive-auth-detail");
        mvc.perform(get("/failure")).andExpect(status().isForbidden());
        failures.failure = new IllegalStateException("sensitive-internal-detail");
        mvc.perform(get("/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("SERVER_ERROR"))
                .andExpect(content().string(not(containsString("sensitive-internal-detail"))));
    }

    @Test
    void invalidReturnValueIsServerErrorNotClientError() throws Exception {
        mvc.perform(get("/invalid-response")).andExpect(status().isInternalServerError());
    }
}
