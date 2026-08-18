package com.example.sodasplash_gateway.filter;

import com.example.sodasplash_gateway.resolver.UserKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimiterFilter implements WebFilter, Ordered {

    private final UserKeyResolver userKeyResolver;
    private final int replenishRate;
    private final int burstCapacity;

    // In-memory token bucket per key
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterFilter(
            UserKeyResolver userKeyResolver,
            @Value("${gateway.rate-limit.replenish-rate:100}") int replenishRate,
            @Value("${gateway.rate-limit.burst-capacity:100}") int burstCapacity
    ) {
        this.userKeyResolver = userKeyResolver;
        this.replenishRate = replenishRate;
        this.burstCapacity = burstCapacity;
    }

    @Override
    public int getOrder() {
        // Run after JwtAuthenticationFilter (which places Authentication in SecurityContext)
        return 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return userKeyResolver.resolve(exchange)
                .defaultIfEmpty("ip:anonymous")
                .flatMap(key -> {
                    TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(burstCapacity, replenishRate));

                    if (bucket.tryConsume()) {
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(burstCapacity));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(bucket.getTokens()));
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded for key: {}", key);
                        return rateLimitExceeded(exchange);
                    }
                });
    }

    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(burstCapacity));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders().add("Retry-After", "60");

        String body = String.format(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"timestamp\":\"%s\"}",
                Instant.now().toString()
        );

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, int refillRatePerMinute) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillRatePerSecond = refillRatePerMinute / 60.0;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized int getTokens() {
            refill();
            return (int) Math.floor(tokens);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double deltaSeconds = (now - lastRefillTimestamp) / 1000.0;
            if (deltaSeconds > 0) {
                tokens = Math.min(capacity, tokens + deltaSeconds * refillRatePerSecond);
                lastRefillTimestamp = now;
            }
        }
    }
}
