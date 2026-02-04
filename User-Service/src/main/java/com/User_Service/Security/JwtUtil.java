package com.User_Service.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility for User Service.
 *
 * CRITICAL: The secret key must be identical in Match Service's JwtUtil.
 * Both services need to validate tokens independently using the same HMAC key.
 *
 * Compatible with io.jsonwebtoken:jjwt-api:0.12.6
 */
@Component
public class JwtUtil {

    private static final String SECRET =
            "indiChess-super-secret-key-indiChess-super-secret-key";

    private final SecretKey key =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * Generate a JWT token for the given email.
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Extract email (subject) from the token.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract username (alias for extractEmail for compatibility).
     * This method exists because Spring Security and some code expects "extractUsername".
     */
    public String extractUsername(String token) {
        return extractEmail(token);
    }

    /**
     * Validate the token against a specific username/email.
     * @param token JWT token to validate
     * @param username Expected username/email
     * @return true if token is valid and matches the username
     */
    public boolean validateToken(String token, String username) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate the token (checks signature and expiration).
     * Overloaded method without username parameter.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the token is expired.
     */
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extract the expiration date from the token.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Parse and extract all claims from the token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}