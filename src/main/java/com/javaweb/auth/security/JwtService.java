package com.javaweb.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        if (properties.accessTokenExpiration() == null
                || properties.accessTokenExpiration().isZero()
                || properties.accessTokenExpiration().isNegative()) {
            throw new IllegalStateException("JWT access token expiration must be positive");
        }

        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = properties.accessTokenExpiration();
        this.clock = clock;
    }

    public String generateAccessToken(AuthUserPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenExpiration);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.id())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, AuthUserPrincipal principal) {
        Claims claims = parseClaims(token);
        Object userId = claims.get("uid");
        return principal.getUsername().equalsIgnoreCase(claims.getSubject())
                && userId instanceof Number number
                && principal.id().equals(number.longValue())
                && claims.getExpiration().toInstant().isAfter(clock.instant());
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenExpiration.toSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
