package com.example.bankingapp;

import com.example.bankingapp.Account.AccountNotFoundException;
import com.example.bankingapp.User.UserNotFoundException;
import com.example.bankingapp.Staff.UsernameAlreadyUsedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class MainExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request parameters are invalid. Account IDs must be positive integers.");
        problem.setTitle("Validation Failed");
        problem.setProperty("errorCode", "BAD_REQUEST");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(UsernameAlreadyUsedException.class)
    public ProblemDetail handleUsernameConflict(UsernameAlreadyUsedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid JSON or unsupported field value.");
    }

    //Entity Errors
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setTitle("User Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "NOT_FOUND");
        return problem;

    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setTitle("Account Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "NOT_FOUND");
        return problem;
    }


    //General Data integrity Exception

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setTitle("Data Conflict");
        problem.setDetail("The record conflicts with existing data or a database constraint.");
        problem.setProperty("errorCode", "CONFLICT");
        return problem;
    }


    //Validation Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidations(MethodArgumentNotValidException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more fields have errors.");

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        problem.setProperty("errors", errors);

        return problem;

        /*Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);*/
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleBadRequest(MethodArgumentTypeMismatchException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setTitle("Incorrect/malformed data");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode","BAD_REQUEST");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralExceptions(Exception ex) {

        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatus(500);
        problem.setTitle("Internal Server Error");
        problem.setDetail("Unexpected Error by the server");
        problem.setProperty("errorCode","SERVER_ERROR");

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more fields are invalid.");

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                         violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        problem.setProperty("errors", errors);
        problem.setProperty("errorCode", "VALIDATION_ERROR");

        return problem;
    }
}
