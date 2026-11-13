package com.example.bankingapp;

import com.example.bankingapp.Account.AccountNotFoundException;
import com.example.bankingapp.User.UserNotFoundException;
import com.example.bankingapp.Staff.UsernameAlreadyUsedException;
import com.example.bankingapp.Auth.SignupConflictException;
import tools.jackson.databind.exc.InvalidFormatException;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.stream.StreamSupport;

@Slf4j
@RestControllerAdvice
public class MainExceptionHandler {
    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("errorCode", code);
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex) {
        if (ex.isForReturnValue()) return handleGeneralExceptions(ex).getBody();
        return problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more request parameters are invalid. IDs must be positive integers.", "BAD_REQUEST");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Access Denied", "Access denied.", "FORBIDDEN");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication Required", "A valid login is required.", "UNAUTHORIZED");
    }

    @ExceptionHandler(UsernameAlreadyUsedException.class)
    public ProblemDetail handleUsernameConflict(UsernameAlreadyUsedException ex) {
        return problem(HttpStatus.CONFLICT, "Username Conflict", ex.getMessage(), "CONFLICT");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidFormatException invalid && invalid.getTargetType() == LocalDate.class) {
                var result = problem(HttpStatus.BAD_REQUEST, "Invalid Date of Birth",
                        "Enter a valid date of birth in YYYY-MM-DD format.", "VALIDATION_ERROR");
                result.setProperty("errors", List.of("dateOfBirth: Enter a valid date of birth in YYYY-MM-DD format."));
                return result;
            }
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid Request Body",
                "Request body is missing, malformed, or contains an unsupported field value.", "BAD_REQUEST");
    }

    @ExceptionHandler(SignupConflictException.class)
    public ProblemDetail handleSignupConflict(SignupConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Signup Failed", ex.getMessage(), "SIGNUP_CONFLICT");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), "NOT_FOUND");
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Account Not Found", ex.getMessage(), "NOT_FOUND");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Data Conflict",
                "The record conflicts with existing data or a database constraint.", "CONFLICT");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Update Conflict",
                "This record changed while your request was being processed. Reload it before making another update.", "CONFLICT");
    }

    @ExceptionHandler({TransientDataAccessException.class, DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class})
    public ProblemDetail handleDatabaseUnavailable(Exception ex) {
        log.warn("Database request unavailable: {}", ex.getClass().getSimpleName());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                "The database is temporarily unavailable. Check whether your request completed before retrying.", "SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ProblemDetail handleValidations(BindException ex) {
        var problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields have errors.", "VALIDATION_ERROR");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).sorted().toList());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleBadRequest(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Parameter",
                "A request parameter has an invalid format.", "BAD_REQUEST");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        boolean invalidReturnValue = ex.getConstraintViolations().stream().anyMatch(violation ->
                StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                        .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE));
        if (invalidReturnValue) return handleGeneralExceptions(ex).getBody();
        var problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid.", "VALIDATION_ERROR");
        // Preserve multiple constraints on the same path without duplicate-key failures.
        problem.setProperty("errors", ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage()).sorted().toList());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralExceptions(Exception ex) {
        // Preserve Spring MVC's status and protocol headers, such as Allow on 405.
        if (ex instanceof ErrorResponse error
                && (error.getStatusCode().is4xxClientError() || error.getStatusCode().value() == 503)) {
            int status = error.getStatusCode().value();
            String detail = switch (status) {
                case 400 -> "A required request parameter is missing or invalid.";
                case 404 -> "The requested resource was not found.";
                case 405 -> "This HTTP method is not supported for this resource.";
                case 406 -> "The requested response format is not supported.";
                case 413 -> "The request is too large.";
                case 415 -> "The request Content-Type is not supported.";
                case 503 -> "The request could not complete because the service is temporarily unavailable.";
                default -> "The request could not be processed.";
            };
            var problem = ProblemDetail.forStatusAndDetail(error.getStatusCode(), detail);
            problem.setProperty("errorCode", "HTTP_" + status);
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders()).body(problem);
        }
        log.error("Unexpected server error", ex);
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", "An unexpected server error occurred.", "SERVER_ERROR"));
    }
}
