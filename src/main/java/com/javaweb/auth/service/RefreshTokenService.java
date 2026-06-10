package com.javaweb.auth.service;

import com.javaweb.auth.entity.RefreshToken;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.RefreshTokenRepository;
import com.javaweb.auth.security.JwtProperties;
import com.javaweb.common.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenExpiration;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties properties
    ) {
        if (properties.refreshTokenExpiration() == null
                || properties.refreshTokenExpiration().isZero()
                || properties.refreshTokenExpiration().isNegative()) {
            throw new IllegalStateException("Refresh token expiration must be positive");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = properties.refreshTokenExpiration();
    }

    public IssuedRefreshToken issue(User user, Instant now) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = now.plus(refreshTokenExpiration);

        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), expiresAt));
        return new IssuedRefreshToken(
                rawToken,
                refreshTokenExpiration.toSeconds()
        );
    }

    public RefreshToken requireActive(String rawToken, Instant now) {
        RefreshToken token = refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        if (token.isRevoked() || token.isExpired(now)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        return token;
    }

    public void revoke(String rawToken, Instant now) {
        refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> token.setRevokedAt(now));
    }

    public void revoke(RefreshToken token, Instant now) {
        token.setRevokedAt(now);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(
            String value,
            long expiresInSeconds
    ) {
    }
}
