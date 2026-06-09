package com.javaweb.auth.repository;

import com.javaweb.auth.entity.OtpToken;
import com.javaweb.auth.enums.OtpTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByUserIdAndTokenTypeAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            OtpTokenType tokenType
    );

    Optional<OtpToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(Instant cutoff);
}
