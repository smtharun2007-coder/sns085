package com.authease.controller;

import com.authease.dto.ErrorResponse;
import com.authease.service.ReasonCatalogService;
import com.authease.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ReasonCatalogService reasonCatalogService;

    public GlobalExceptionHandler(ReasonCatalogService reasonCatalogService) {
        this.reasonCatalogService = reasonCatalogService;
    }

    @ExceptionHandler(UserService.CustomAuthException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuth(UserService.CustomAuthException ex) {
        ErrorResponse error = new ErrorResponse(ex.getReasonCode(), ex.getTitle(), ex.getMessage(), ex.getNextStep());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("UNAUTHORIZED".equalsIgnoreCase(ex.getReasonCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else if ("FORBIDDEN".equalsIgnoreCase(ex.getReasonCode()) || "ADMIN_MFA_REQUIRED".equalsIgnoreCase(ex.getReasonCode())) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "Invalid input parameters";

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Please check your input",
                message,
                "Correct the highlighted details and try again."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                "Method Not Allowed",
                "The requested HTTP method is not supported for this endpoint.",
                "Submit the request using the designated HTTP method (POST)."
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "SERVER_ERROR",
                "Something went wrong",
                "We encountered an unexpected error processing your request.",
                "Please refresh the page and try again in a few moments."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
