package com.javaweb.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        String apiKey,
        String model,
        String baseUrl,
        Duration timeout
) {
    public AiProperties {
        provider = provider == null || provider.isBlank() ? "openai" : provider;
        apiKey = apiKey == null ? "" : apiKey;
        model = model == null || model.isBlank() ? "not-configured" : model;
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
