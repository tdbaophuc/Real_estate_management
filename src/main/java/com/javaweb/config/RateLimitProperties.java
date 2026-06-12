package com.javaweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Duration window,
        int authRequests,
        int aiRequests
) {
    public RateLimitProperties {
        window = window == null || window.isZero() || window.isNegative()
                ? Duration.ofMinutes(1)
                : window;
        authRequests = authRequests <= 0 ? 20 : authRequests;
        aiRequests = aiRequests <= 0 ? 60 : aiRequests;
    }
}
