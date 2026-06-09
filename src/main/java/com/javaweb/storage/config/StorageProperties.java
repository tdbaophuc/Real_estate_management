package com.javaweb.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.Set;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        Path localRoot,
        DataSize maxFileSize,
        Set<String> allowedContentTypes
) {
    public StorageProperties {
        localRoot = localRoot == null ? Path.of("./var/storage") : localRoot;
        maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(10) : maxFileSize;
        allowedContentTypes = allowedContentTypes == null || allowedContentTypes.isEmpty()
                ? Set.of("image/jpeg", "image/png", "image/webp", "application/pdf")
                : Set.copyOf(allowedContentTypes);
    }
}
