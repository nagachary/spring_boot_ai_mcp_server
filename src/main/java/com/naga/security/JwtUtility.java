package com.naga.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtility {

    @Value("${mcp.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${mcp.auth.jwt-ttl-seconds}")
    private long ttl;

    @Value("${mcp.auth.refresh-window-seconds}")
    private long refreshWindow;

    @Value("${mcp.auth.token.issuer}")
    private String tokenIssuer;

    private final Key key;

    public JwtUtility(@Value("${mcp.auth.jwt-secret}") String base64Secret) {
        this.key = Keys.hmacShaKeyFor(
                base64Secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(tokenIssuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validate(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                //.setSigningKey(jwtSecret.getBytes())
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean shouldRefresh(Claims claims) {
        return claims.getExpiration()
                .toInstant()
                .minusSeconds(refreshWindow)
                .isBefore(Instant.now());
    }
}
