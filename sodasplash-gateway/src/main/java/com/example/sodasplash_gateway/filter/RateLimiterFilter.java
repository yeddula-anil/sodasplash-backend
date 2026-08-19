package com.example.sodasplash_gateway.filter;

import com.example.sodasplash_gateway.resolver.UserKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class RateLimiterFilter implements WebFilter, Ordered {

    private static final RedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refillRate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local state = redis.call('HMGET', key, 'tokens', 'timestamp')
            local tokens = tonumber(state[1])
            local lastRefill = tonumber(state[2])
            if tokens == nil or lastRefill == nil then
                tokens = capacity
                lastRefill = now
            else
                local elapsedSeconds = math.max(0, now - lastRefill) / 1000
                tokens = math.min(capacity, tokens + elapsedSeconds * refillRate)
                lastRefill = now
            end
            local allowed = 0
            local requestedTokens = tonumber(ARGV[5])
            if tokens >= requestedTokens then
                tokens = tokens - requestedTokens
                allowed = 1
            end
            redis.call('HSET', key, 'tokens', tokens, 'timestamp', lastRefill)
            redis.call('EXPIRE', key, ttl)
            return {allowed, math.floor(tokens)}
            """, List.class);

    private final UserKeyResolver userKeyResolver;
    private final int replenishRate;
    private final int burstCapacity;
    private final int requestedTokens;
    private final int bucketTtlSeconds;
    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimiterFilter(
            UserKeyResolver userKeyResolver,
            @Value("${gateway.rate-limit.replenish-rate:100}") int replenishRate,
            @Value("${gateway.rate-limit.burst-capacity:100}") int burstCapacity,
            @Value("${gateway.rate-limit.requested-tokens:1}") int requestedTokens,
            ReactiveStringRedisTemplate redisTemplate
    ) {
        this.userKeyResolver = userKeyResolver;
        if (replenishRate <= 0 || burstCapacity <= 0 || requestedTokens <= 0) {
            throw new IllegalArgumentException("Rate-limit settings must be positive");
        }
        this.replenishRate = replenishRate;
        this.burstCapacity = burstCapacity;
        this.requestedTokens = requestedTokens;
        this.bucketTtlSeconds = Math.max(60, (int) Math.ceil((double) burstCapacity / replenishRate * 2));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        // Run after JwtAuthenticationFilter (which places Authentication in SecurityContext)
        return 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // A CORS preflight does not represent a protected API operation and
        // must remain available even when the rate-limit backend is unhealthy.
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        return userKeyResolver.resolve(exchange)
                .defaultIfEmpty("ip:anonymous")
                .flatMap(key -> consumeToken(key)
                        .flatMap(result -> {
                    if (result.allowed()) {
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(burstCapacity));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
                        return chain.filter(exchange);
                    }
                        log.warn("Rate limit exceeded for key: {}", key);
                        return rateLimitExceeded(exchange);
                })
                .onErrorResume(error -> {
                    log.error("Upstash rate-limit request failed", error);
                    return rateLimitServiceUnavailable(exchange);
                }));
    }

    private Mono<RateLimitResult> consumeToken(String key) {
        return redisTemplate.execute(
                        TOKEN_BUCKET_SCRIPT,
                        List.of("rate-limit:" + key),
                        List.of(
                burstCapacity, replenishRate / 60.0, System.currentTimeMillis(), bucketTtlSeconds, requestedTokens
                        )
                )
                .single()
                .map(result -> {
                    if (result.size() != 2) {
                        throw new IllegalStateException("Unexpected response from Upstash rate limiter");
                    }
                    return new RateLimitResult(asInteger(result.get(0)) == 1, asInteger(result.get(1)));
                });
    }

    private int asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Unexpected response from Upstash rate limiter", exception);
        }
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

    private Mono<Void> rateLimitServiceUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("Retry-After", "2");
        byte[] bytes = "{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"Rate limiting service is temporarily unavailable.\"}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private record RateLimitResult(boolean allowed, int remainingTokens) { }
}
