package com.example.bankingapp;

import com.example.bankingapp.User.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class MainExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
       return ResponseEntity.status(404).body(ex.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidations(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
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
