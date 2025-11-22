package com.example.chronos.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private final javax.crypto.SecretKey key = Keys.hmacShaKeyFor("super-secret-demo-key-chronos-12345678901234567890".getBytes());
    private static final long EXP_MS = 24 * 60 * 60 * 1000L;

    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXP_MS);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
