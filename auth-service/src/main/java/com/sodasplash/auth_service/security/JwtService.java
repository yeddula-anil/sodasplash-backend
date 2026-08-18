package com.sodasplash.auth_service.security;


import com.sodasplash.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expiration;


    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiration = expiration;
    }


    public String generateToken(User user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + expiration
        );


        return Jwts.builder()

                .subject(
                        user.getId().toString()
                )

                .claim(
                        "email",
                        user.getEmail()
                )

                .claim(
                        "username",
                        user.getUsername()
                )

                .claim(
                        "role",
                        user.getRole().name()
                )

                .issuedAt(now)

                .expiration(expiry)

                .signWith(secretKey)

                .compact();
    }


    public Claims extractAllClaims(String token) {

        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    public String extractUserId(String token) {

        return extractAllClaims(token)
                .getSubject();
    }


    public boolean isTokenValid(String token) {

        try {

            Claims claims =
                    extractAllClaims(token);

            return claims
                    .getExpiration()
                    .after(new Date());

        } catch (Exception e) {

            return false;
        }
    }
}
