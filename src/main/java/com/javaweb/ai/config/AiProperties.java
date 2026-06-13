package com.javaweb.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        String apiKey,
        String model,
        Duration timeout
) {
    public AiProperties {
        provider = provider == null || provider.isBlank() ? "noop" : provider;
        apiKey = apiKey == null ? "" : apiKey;
        model = model == null || model.isBlank() ? "not-configured" : model;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
