package com.sodasplash.auth_service.config;

import com.sodasplash.auth_service.security.JwtAuthenticationFilter;
import com.sodasplash.auth_service.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;


    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =====================================================
                // CSRF
                // =====================================================

                .csrf(csrf ->
                        csrf.disable()
                )


                // =====================================================
                // SESSION
                // =====================================================

                /*
                 * Google OAuth2 requires a temporary HTTP session
                 * during the authorization flow.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )


                // =====================================================
                // AUTHORIZATION
                // =====================================================

                .authorizeHttpRequests(auth ->
                        auth

                                // =============================================
                                // CORS PREFLIGHT
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()


                                // =============================================
                                // PUBLIC - EMAIL LOGIN
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/login"
                                )
                                .permitAll()


                                // =============================================
                                // PUBLIC - EMAIL REGISTER
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/register"
                                )
                                .permitAll()


                                // =============================================
                                // PUBLIC - BD USER LIST
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/auth/users/bd"
                                )
                                .permitAll()


                                // =============================================
                                // GOOGLE OAUTH
                                // =============================================

                                .requestMatchers(
                                        "/oauth2/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/login/**"
                                )
                                .permitAll()


                                // =============================================
                                // ADMIN - STAFF
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/auth/staff"
                                )
                                .hasRole("ADMIN")


                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/staff"
                                )
                                .hasRole("ADMIN")


                                // =============================================
                                // ADMIN - USER MANAGEMENT
                                // =============================================

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/auth/users/*/toggle-status"
                                )
                                .hasRole("ADMIN")


                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/auth/users/*/toggle-role"
                                )
                                .hasRole("ADMIN")


                                // =============================================
                                // EVERYTHING ELSE
                                // =============================================

                                .anyRequest()
                                .authenticated()
                )


                // =====================================================
                // GOOGLE OAUTH2 LOGIN
                // =====================================================

                .oauth2Login(oauth ->
                        oauth

                                .userInfoEndpoint(userInfo ->
                                        userInfo.userService(
                                                oauth2UserService()
                                        )
                                )

                                .successHandler(
                                        googleSuccessHandler()
                                )
                );


        // =====================================================
        // JWT AUTHENTICATION FILTER
        // =====================================================

        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );


        return http.build();
    }


    // =====================================================
    // GOOGLE USER SERVICE
    // =====================================================

    @Bean
    public OAuth2UserService<
            OAuth2UserRequest,
            OAuth2User
            > oauth2UserService() {

        DefaultOAuth2UserService delegate =
                new DefaultOAuth2UserService();


        return userRequest -> {

            OAuth2User oauth2User =
                    delegate.loadUser(userRequest);


            String registrationId =
                    userRequest
                            .getClientRegistration()
                            .getRegistrationId();


            // =================================================
            // GOOGLE
            // =================================================

            if ("google".equals(registrationId)) {

                String email =
                        oauth2User.getAttribute("email");


                Boolean emailVerified =
                        oauth2User.getAttribute(
                                "email_verified"
                        );


                // ---------------------------------------------
                // EMAIL REQUIRED
                // ---------------------------------------------

                if (email == null || email.isBlank()) {

                    throw new RuntimeException(
                            "Google account does not contain an email"
                    );
                }


                // ---------------------------------------------
                // EMAIL MUST BE VERIFIED
                // ---------------------------------------------

                if (!Boolean.TRUE.equals(emailVerified)) {

                    throw new RuntimeException(
                            "Google email is not verified"
                    );
                }
            }


            return oauth2User;
        };
    }


    // =====================================================
    // GOOGLE SUCCESS HANDLER
    // =====================================================

    @Bean
    public AuthenticationSuccessHandler
    googleSuccessHandler() {

        return (
                request,
                response,
                authentication
        ) -> {

            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken)
                            authentication;


            OAuth2User oauthUser =
                    oauthToken.getPrincipal();


            // =================================================
            // GOOGLE USER DETAILS
            // =================================================

            String email =
                    oauthUser.getAttribute("email");


            String name =
                    oauthUser.getAttribute("name");


            String googleId =
                    oauthUser.getAttribute("sub");


            // =================================================
            // LOGIN / REGISTER GOOGLE USER
            // =================================================

            var authResponse =
                    authService.processGoogleUser(
                            email,
                            name,
                            googleId
                    );


            // =================================================
            // REDIRECT TO FRONTEND
            // =================================================

            String redirectUrl =
                    frontendUrl
                            + "/oauth-success#token="
                            + authResponse.getToken();


            response.sendRedirect(
                    redirectUrl
            );
        };
    }
}