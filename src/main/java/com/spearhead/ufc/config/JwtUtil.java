package com.spearhead.ufc.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Date;
import java.util.Base64;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:UUJDVGVzdEtQSUNvbXBsaWFuY2VCYWNrZW5kU2VjcmV0S2V5MjAyNFNwZWFyaGVhZFVGQw==}")
    private String jwtSecret;
    
    private final long accessTokenExpiration = 1000 * 60 * 60; // 1 hour
    private final long refreshTokenExpiration = 1000 * 60 * 60 * 24; // 24 hours

    private Key getSigningKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(jwtSecret);
            return Keys.hmacShaKeyFor(decodedKey);
        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT secret key format: {}", e.getMessage());
            // Fallback to dynamic key if secret is invalid
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }


    // public String generateToken(String userId) {
    //     return Jwts.builder()
    //             .setSubject(userId)
    //             .setIssuedAt(new Date())
    //             .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
    //             .signWith(key)
    //             .compact();
    // }

    // public String generateRefreshToken(String userId) {
    //     return Jwts.builder()
    //             .setSubject(userId)
    //             .setIssuedAt(new Date())
    //             .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
    //             .signWith(key)
    //             .compact();
    // }

    // Get userId from token
    // public String getUserId(String token) {
    //     return Jwts.parserBuilder().setSigningKey(key).build()
    //             .parseClaimsJws(token)
    //             .getBody()
    //             .getSubject();
    // }

    // public boolean validateToken(String token, String userId) {
    //     return userId.equals(getUserId(token)) && !isTokenExpired(token);
    // }

    // private boolean isTokenExpired(String token) {
    //     Date expirationDate = Jwts.parserBuilder().setSigningKey(key).build()
    //             .parseClaimsJws(token)
    //             .getBody()
    //             .getExpiration();
    //     return expirationDate.before(new Date());
    // }
    public String generateToken(int userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(int userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed JWT token", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("JWT claims string is empty or null", e);
        }
    }

    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsername(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (JwtException e) {
            // Log or handle exception as needed
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expirationDate = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expirationDate.before(new Date());
        } catch (JwtException e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

}
