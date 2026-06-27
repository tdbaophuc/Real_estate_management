package com.javaweb.auth.dto;

import com.javaweb.auth.entity.RefreshToken;

import java.time.Instant;

public record SessionResponse(
        Long id,
        Instant createdAt,
        Instant expiresAt
) {
    public static SessionResponse from(RefreshToken token) {
        return new SessionResponse(
                token.getId(),
                token.getCreatedAt(),
                token.getExpiresAt()
        );
    }
}
