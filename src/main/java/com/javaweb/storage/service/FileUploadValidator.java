package com.javaweb.storage.service;

import com.javaweb.storage.config.StorageProperties;
import com.javaweb.storage.exception.FileUploadException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileUploadValidator {
    private static final Set<String> IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp"),
            "application/pdf", Set.of("pdf")
    );

    private final StorageProperties properties;

    public FileUploadValidator(StorageProperties properties) {
        this.properties = properties;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File must not be empty");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new FileUploadException(
                    "File size must not exceed " + properties.maxFileSize().toMegabytes() + " MB"
            );
        }

        String rawName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!StringUtils.hasText(rawName)
                || rawName.contains("..")
                || rawName.contains("/")
                || rawName.contains("\\")) {
            throw new FileUploadException("File name is invalid");
        }
        String originalName = StringUtils.cleanPath(rawName);
        if (originalName.length() > 255) {
            throw new FileUploadException("File name must not exceed 255 characters");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!properties.allowedContentTypes().contains(contentType)) {
            throw new FileUploadException("File type is not allowed");
        }

        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null
                || !ALLOWED_EXTENSIONS.getOrDefault(contentType, Set.of())
                        .contains(extension.toLowerCase(Locale.ROOT))) {
            throw new FileUploadException("File extension does not match its content type");
        }
        validateSignature(file, contentType);
    }

    public void validateImage(MultipartFile file) {
        validate(file);
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new FileUploadException("Property images must use an allowed image type");
        }
    }

    private void validateSignature(MultipartFile file, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (contentType) {
                case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case "image/png" -> startsWith(
                        header,
                        0x89,
                        0x50,
                        0x4E,
                        0x47,
                        0x0D,
                        0x0A,
                        0x1A,
                        0x0A
                );
                case "image/webp" -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                        && bytesEqual(header, 8, 0x57, 0x45, 0x42, 0x50);
                case "application/pdf" -> startsWith(header, 0x25, 0x50, 0x44, 0x46);
                default -> false;
            };
            if (!valid) {
                throw new FileUploadException("File content does not match its content type");
            }
        } catch (IOException exception) {
            throw new FileUploadException("File content could not be inspected", exception);
        }
    }

    private boolean startsWith(byte[] actual, int... expected) {
        return bytesEqual(actual, 0, expected);
    }

    private boolean bytesEqual(byte[] actual, int offset, int... expected) {
        if (actual.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((actual[offset + index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }
}
