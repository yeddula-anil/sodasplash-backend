package com.example.sodasplash_gateway.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {
        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    public Claims extractAllClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(
            String token
    ) {

        try {

            Claims claims =
                    extractAllClaims(token);

            return claims.getExpiration()
                    .after(
                            new java.util.Date()
                    );

        } catch (Exception e) {

            return false;
        }
    }

    public String extractUserId(
            String token
    ) {

        return extractAllClaims(token)
                .getSubject();
    }

    public String extractRole(
            String token
    ) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    public String extractEmail(
            String token
    ) {

        return extractAllClaims(token)
                .get("email", String.class);
    }
}
