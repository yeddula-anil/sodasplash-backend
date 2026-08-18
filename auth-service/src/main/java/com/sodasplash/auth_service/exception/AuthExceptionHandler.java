package com.sodasplash.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AuthExceptionHandler {

    // Known safe, user-facing business error messages
    private static final java.util.Set<String> SAFE_MESSAGES = java.util.Set.of(
            "Invalid email or password",
            "Email is already registered",
            "Username is already taken",
            "Your account has been deactivated",
            "This account was created using Google. Please login with Google.",
            "Only ADMIN or BD accounts can be created",
            "Only BD and ADMIN roles can be toggled",
            "User not found"
    );

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String raw = ex.getMessage();

        // Pass through known safe messages; block raw SQL/stack traces
        String message = (raw != null && isSafeMessage(raw))
                ? raw
                : "An unexpected error occurred. Please try again.";

        HttpStatus status = "Invalid email or password".equalsIgnoreCase(message)
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", firstError
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred. Please try again later."
        ));
    }

    private boolean isSafeMessage(String msg) {
        // Block anything that leaks SQL, class names, or connection details
        String lower = msg.toLowerCase();
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("hibernate")
                || lower.contains("exception") || lower.contains("stacktrace")
                || lower.contains("connection refused") || lower.contains("at com.")
                || lower.contains("at org.")) {
            return false;
        }
        return SAFE_MESSAGES.contains(msg);
    }
}
