package com.blamex321.auth_service.utility;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;


@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // Accept either `jwt.secret` or `spring.jwt.secret` (YAML uses spring.jwt)
    @Value("${jwt.secret:${spring.jwt.secret:}}")
    private String secret;

    // Accept either `jwt.expiration` or `spring.jwt.expiration`, default to 86400000
    @Value("${jwt.expiration:${spring.jwt.expiration:86400000}}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            log.warn("jwt.secret is not configured — generating a temporary key for development (NOT FOR PROD)");
            // generate random key (sized for HS256)
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            return;
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        try {
            // Keys.hmacShaKeyFor requires sufficient key length; fallback if too short
            if (secretBytes.length < 32) {
                log.warn("jwt.secret is too short for HS256 ({} bytes). Generating a fallback secure key. Replace with a longer secret.", secretBytes.length);
                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            } else {
                this.key = Keys.hmacShaKeyFor(secretBytes);
            }
        } catch (Exception e) {
            log.error("Failed to initialize JWT signing key from configured secret. Falling back to generated key.", e);
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}