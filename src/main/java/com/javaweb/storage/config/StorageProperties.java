package com.javaweb.storage.config;

import com.javaweb.storage.enums.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        StorageProvider provider,
        Path localRoot,
        String bucket,
        URI endpoint,
        String region,
        String accessKey,
        String secretKey,
        String publicBaseUrl,
        Duration signedUrlTtl,
        DataSize maxFileSize,
        Set<String> allowedContentTypes
) {
    public StorageProperties {
        provider = provider == null ? StorageProvider.LOCAL : provider;
        localRoot = localRoot == null ? Path.of("./var/storage") : localRoot;
        region = hasText(region) ? region : "auto";
        accessKey = accessKey == null ? "" : accessKey;
        secretKey = secretKey == null ? "" : secretKey;
        publicBaseUrl = publicBaseUrl == null ? "" : trimTrailingSlash(publicBaseUrl);
        signedUrlTtl = signedUrlTtl == null ? Duration.ofMinutes(15) : signedUrlTtl;
        maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(10) : maxFileSize;
        allowedContentTypes = allowedContentTypes == null || allowedContentTypes.isEmpty()
                ? Set.of("image/jpeg", "image/png", "image/webp", "application/pdf")
                : Set.copyOf(allowedContentTypes);
        if (provider == StorageProvider.R2) {
            requireText(bucket, "app.storage.bucket is required when app.storage.provider=r2");
            requirePresent(endpoint, "app.storage.endpoint is required when app.storage.provider=r2");
            requireText(accessKey, "app.storage.access-key is required when app.storage.provider=r2");
            requireText(secretKey, "app.storage.secret-key is required when app.storage.provider=r2");
        }
        if (signedUrlTtl.isZero() || signedUrlTtl.isNegative()) {
            throw new IllegalArgumentException("app.storage.signed-url-ttl must be positive");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePresent(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
