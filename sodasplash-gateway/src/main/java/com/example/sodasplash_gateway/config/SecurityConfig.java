package com.example.sodasplash_gateway.config;

import com.example.sodasplash_gateway.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {

        return http

                // =====================================================
                // CSRF
                // =====================================================

                .csrf(ServerHttpSecurity.CsrfSpec::disable)


                // =====================================================
                // JWT AUTHENTICATION
                // =====================================================

                .addFilterAt(
                        jwtAuthenticationFilter,
                        org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
                )


                // =====================================================
                // AUTHORIZATION
                // =====================================================

                .authorizeExchange(exchanges -> exchanges


                        // =================================================
                        // CORS PREFLIGHT
                        // =================================================

                        .pathMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()


                        // =================================================
                        // PUBLIC AUTH
                        // =================================================

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/register"
                        )
                        .permitAll()


                        // =================================================
                        // GOOGLE OAUTH
                        // =================================================

                        .pathMatchers(
                                "/oauth2/**"
                        )
                        .permitAll()

                        .pathMatchers(
                                "/login/oauth2/**"
                        )
                        .permitAll()


                        // =================================================
                        // ADMIN - AUTH SERVICE
                        // =================================================

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/auth/staff"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/staff"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/auth/users/*/toggle-status"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/auth/users/*/toggle-role"
                        )
                        .hasRole("ADMIN")


                        // =================================================
                        // ADMIN - PRODUCTS
                        // =================================================

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/products"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/products/*"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/products/*/toggle-status"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/products/*"
                        )
                        .hasRole("ADMIN")


                        // =================================================
                        // ADMIN - FLAVOURS
                        // =================================================

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/products/*/flavours"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/products/*/flavours/*"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/products/*/flavours/*/toggle-status"
                        )
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/products/*/flavours/*"
                        )
                        .hasRole("ADMIN")


                        // =================================================
                        // ADMIN - GET ALL ORDERS
                        // =================================================

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders"
                        )
                        .hasRole("ADMIN")


                        // =================================================
                        // CUSTOMER - GET ORDERS BY EMAIL
                        // =================================================

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/by-email"
                        )
                        .hasRole("CUSTOMER")


                        // =================================================
                        // BD - ORDERS BY REFERRAL EMAIL
                        // =================================================

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/by-referral-email"
                        )
                        .hasRole("BD")


                        // =================================================
                        // ADMIN + BD - SINGLE ORDER
                        // =================================================

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/*"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "BD"
                        )


                        // =================================================
                        // ADMIN + BD - UPDATE ORDER
                        // =================================================

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/orders/*"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "BD"
                        )


                        // =================================================
                        // ADMIN + BD - UPDATE STAGE
                        // =================================================

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/orders/*/stage"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "BD"
                        )


                        // =================================================
                        // ADMIN + BD - CANCEL
                        // =================================================

                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/orders/*/cancel"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "BD"
                        )


                        // =================================================
                        // ADMIN + BD - INVOICE
                        // =================================================

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/orders/*/invoice"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "BD"
                        )


                        // =================================================
                        // PUBLIC - CREATE ORDER
                        // =================================================

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/orders"
                        )
                        .permitAll()


                        // =================================================
                        // EVERYTHING ELSE
                        // =================================================

                        .anyExchange()
                        .permitAll()
                )

                .build();
    }
}