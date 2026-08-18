package com.example.sodasplash_gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@Order(-2) // High priority error handler in WebFlux
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String errorTitle;
        String userFriendlyMessage;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorTitle = status.getReasonPhrase();
            userFriendlyMessage = sanitizeMessage(rse.getReason(), status);
        } else if (ex instanceof ConnectException || ex.getCause() instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorTitle = "Service Unavailable";
            userFriendlyMessage = "The requested service is currently unavailable. Please try again later.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorTitle = "Internal Server Error";
            userFriendlyMessage = "An unexpected error occurred while processing your request.";
        }

        log.error("Gateway error intercepted: Status {} - Error: {} - Original exception: {}",
                status.value(), errorTitle, ex.getMessage());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(),
                escapeJson(errorTitle),
                escapeJson(userFriendlyMessage),
                Instant.now().toString()
        );

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String sanitizeMessage(String originalReason, HttpStatus status) {
        if (originalReason == null || originalReason.isBlank()) {
            return switch (status) {
                case UNAUTHORIZED -> "Authentication token is missing or invalid.";
                case FORBIDDEN -> "You do not have permission to access this resource.";
                case NOT_FOUND -> "The requested API endpoint was not found.";
                case TOO_MANY_REQUESTS -> "Rate limit exceeded. Please slow down your requests.";
                case SERVICE_UNAVAILABLE -> "Service is temporarily unavailable.";
                default -> "An error occurred while processing your request.";
            };
        }

        // Hide raw exception messages, class names, connection details
        String lower = originalReason.toLowerCase();
        if (lower.contains("connection refused") || lower.contains("connectexception") || lower.contains("socketexception")) {
            return "Unable to reach the backend service. Please try again later.";
        }
        if (lower.contains("sql") || lower.contains("database") || lower.contains("psqlexception") || lower.contains("hibernate")) {
            return "A database operation failed. Please try again later.";
        }

        return originalReason;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
