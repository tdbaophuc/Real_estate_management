package com.javaweb.storage.service;

import com.javaweb.storage.config.StorageProperties;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import com.javaweb.storage.exception.FileUploadException;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public class R2FileStorageService implements FileStorageService {
    private final StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public R2FileStorageService(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.presigner = S3Presigner.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region().toLowerCase(Locale.ROOT)))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.R2;
    }

    @Override
    public StoredFile store(MultipartFile file, FileAccessLevel accessLevel) {
        String storageKey = generateStorageKey(file, accessLevel);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .contentLength(file.getSize())
                    .contentType(file.getContentType())
                    .build();
            try (InputStream source = file.getInputStream();
                 DigestInputStream digested = new DigestInputStream(source, digest)) {
                s3Client.putObject(request, RequestBody.fromInputStream(digested, file.getSize()));
            }
            return new StoredFile(
                    storageKey,
                    HexFormat.of().formatHex(digest.digest()),
                    publicUrl(storageKey, accessLevel)
            );
        } catch (IOException | NoSuchAlgorithmException | S3Exception exception) {
            deleteAfterFailedStore(storageKey);
            throw new FileUploadException("File could not be stored in R2", exception);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(request, ResponseTransformer.toInputStream());
            return new InputStreamResource(response);
        } catch (NoSuchKeyException exception) {
            throw new FileUploadException("Stored file was not found", exception);
        } catch (S3Exception exception) {
            throw new FileUploadException("Stored file could not be read from R2", exception);
        }
    }

    @Override
    public String createDownloadUrl(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.signedUrlTtl())
                .getObjectRequest(request)
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public StoredFile changeAccessLevel(String storageKey, FileAccessLevel accessLevel) {
        return new StoredFile(storageKey, checksum(storageKey), publicUrl(storageKey, accessLevel));
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
        } catch (NoSuchKeyException ignored) {
            // Delete remains idempotent for callers cleaning up failed persistence.
        } catch (S3Exception exception) {
            throw new FileUploadException("Stored file could not be deleted from R2", exception);
        }
    }

    private String generateStorageKey(MultipartFile file, FileAccessLevel accessLevel) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        return String.join(
                "/",
                accessLevel.name().toLowerCase(Locale.ROOT),
                Integer.toString(today.getYear()),
                "%02d".formatted(today.getMonthValue()),
                UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT)
        );
    }

    private String publicUrl(String storageKey, FileAccessLevel accessLevel) {
        if (accessLevel != FileAccessLevel.PUBLIC || properties.publicBaseUrl().isBlank()) {
            return null;
        }
        return properties.publicBaseUrl() + "/" + storageKey;
    }

    private String checksum(String storageKey) {
        try (InputStream source = load(storageKey).getInputStream();
             DigestInputStream digested = new DigestInputStream(
                     source,
                     MessageDigest.getInstance("SHA-256")
             )) {
            digested.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digested.getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new FileUploadException("Stored file checksum could not be calculated", exception);
        }
    }

    private void deleteAfterFailedStore(String storageKey) {
        try {
            delete(storageKey);
        } catch (FileUploadException ignored) {
            // Preserve the original upload failure.
        }
    }

    @PreDestroy
    void close() {
        presigner.close();
        s3Client.close();
    }
}
