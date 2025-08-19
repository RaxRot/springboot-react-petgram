package com.raxrot.back.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse build(HttpStatus status, String message, HttpServletRequest req,
                                Map<String, String> fieldErrors) {
        return new ErrorResponse(
                java.time.OffsetDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                fieldErrors
        );
    }

    // Custom API exceptions carrying their own HttpStatus
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        log.warn("ApiException: {}", ex.getMessage());
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(build(status, ex.getMessage(), req, null), status);
    }

    // @Valid on @RequestBody field errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest req) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        return new ResponseEntity<>(build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields),
                HttpStatus.BAD_REQUEST);
    }

    // Bad JSON / empty body / wrong content type
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        return new ResponseEntity<>(build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req, null),
                HttpStatus.BAD_REQUEST);
    }

    // Type mismatch in query/path variables (?page=abc, id=xyz)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String msg = "Parameter '%s' value '%s' is invalid".formatted(
                ex.getName(), ex.getValue());
        return new ResponseEntity<>(build(HttpStatus.BAD_REQUEST, msg, req, null),
                HttpStatus.BAD_REQUEST);
    }

    // Access denied by Spring Security
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest req) {
        return new ResponseEntity<>(build(HttpStatus.FORBIDDEN, "Access denied", req, null),
                HttpStatus.FORBIDDEN);
    }

    // Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error", ex);
        return new ResponseEntity<>(build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req, null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}