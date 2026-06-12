package com.javaweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials,
        long maxAgeSeconds
) {
    public CorsProperties {
        allowedOrigins = clean(allowedOrigins);
        allowedMethods = allowedMethods == null || allowedMethods.isEmpty()
                ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                : clean(allowedMethods);
        allowedHeaders = allowedHeaders == null || allowedHeaders.isEmpty()
                ? List.of("Authorization", "Content-Type")
                : clean(allowedHeaders);
        maxAgeSeconds = maxAgeSeconds <= 0 ? 3600 : maxAgeSeconds;
    }

    private static List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
