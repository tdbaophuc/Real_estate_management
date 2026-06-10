package com.javaweb.storage.service;

public record StoredFile(
        String storageKey,
        String checksumSha256,
        String publicUrl
) {
}
