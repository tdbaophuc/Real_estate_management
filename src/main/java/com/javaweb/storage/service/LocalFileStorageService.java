package com.javaweb.storage.service;

import com.javaweb.storage.config.StorageProperties;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import com.javaweb.storage.exception.FileUploadException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public class LocalFileStorageService implements FileStorageService {
    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.root = properties.localRoot().toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve("public"));
            Files.createDirectories(root.resolve("private"));
        } catch (IOException exception) {
            throw new FileUploadException("Local storage could not be initialized", exception);
        }
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public StoredFile store(MultipartFile file, FileAccessLevel accessLevel) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        String relativeKey = String.join(
                "/",
                accessLevel.name().toLowerCase(Locale.ROOT),
                Integer.toString(today.getYear()),
                "%02d".formatted(today.getMonthValue()),
                UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT)
        );
        Path target = resolveKey(relativeKey);

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream source = file.getInputStream();
                 DigestInputStream digested = new DigestInputStream(source, digest)) {
                Files.copy(digested, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String publicUrl = accessLevel == FileAccessLevel.PUBLIC
                    ? "/uploads/" + relativeKey.substring("public/".length())
                    : null;
            return new StoredFile(
                    relativeKey,
                    HexFormat.of().formatHex(digest.digest()),
                    publicUrl
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteIfPresent(target);
            throw new FileUploadException("File could not be stored", exception);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Resource resource = new UrlResource(resolveKey(storageKey).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileUploadException("Stored file was not found");
            }
            return resource;
        } catch (IOException exception) {
            throw new FileUploadException("Stored file could not be read", exception);
        }
    }

    @Override
    public String createDownloadUrl(String storageKey) {
        return null;
    }

    @Override
    public StoredFile changeAccessLevel(String storageKey, FileAccessLevel accessLevel) {
        String currentPrefix = storageKey.startsWith("public/")
                ? "public"
                : "private";
        String targetPrefix = accessLevel.name().toLowerCase(Locale.ROOT);
        String targetKey = targetPrefix + storageKey.substring(currentPrefix.length());
        if (targetKey.equals(storageKey)) {
            return new StoredFile(storageKey, checksum(resolveKey(storageKey)), publicUrl(targetKey, accessLevel));
        }
        Path source = resolveKey(storageKey);
        Path target = resolveKey(targetKey);
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(targetKey, checksum(target), publicUrl(targetKey, accessLevel));
        } catch (IOException exception) {
            throw new FileUploadException("Stored file access level could not be changed", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        deleteIfPresent(resolveKey(storageKey));
    }

    private Path resolveKey(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new FileUploadException("Storage key is invalid");
        }
        return resolved;
    }

    private void deleteIfPresent(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new FileUploadException("Stored file could not be deleted", exception);
        }
    }

    private String publicUrl(String storageKey, FileAccessLevel accessLevel) {
        return accessLevel == FileAccessLevel.PUBLIC
                ? "/uploads/" + storageKey.substring("public/".length())
                : null;
    }

    private String checksum(Path path) {
        try (InputStream input = Files.newInputStream(path);
             DigestInputStream digested = new DigestInputStream(
                     input,
                     MessageDigest.getInstance("SHA-256")
             )) {
            digested.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digested.getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new FileUploadException("Stored file checksum could not be calculated", exception);
        }
    }
}
