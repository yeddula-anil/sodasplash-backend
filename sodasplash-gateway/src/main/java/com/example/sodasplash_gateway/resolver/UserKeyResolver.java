package com.example.sodasplash_gateway.resolver;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("userKeyResolver")
public class UserKeyResolver
        implements KeyResolver {

    @Override
    public Mono<String> resolve(
            org.springframework.web.server.ServerWebExchange exchange
    ) {

        return exchange
                .getPrincipal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .map(userId -> "user:" + userId)
                .switchIfEmpty(Mono.defer(() -> {
                    String ip = "anonymous";
                    if (exchange.getRequest().getRemoteAddress() != null && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                        ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                    }
                    return Mono.just("ip:" + ip);
                }));
    }
}
