package com.javaweb.storage.service;

import com.javaweb.storage.config.StorageConfig;
import com.javaweb.storage.config.StorageProperties;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import com.javaweb.storage.exception.FileUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class R2FileStorageServiceTest {
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00
    };

    @Test
    void storeShouldUploadToConfiguredBucketAndReturnR2Metadata() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    RequestBody body = invocation.getArgument(1);
                    try (var input = body.contentStreamProvider().newStream()) {
                        input.readAllBytes();
                    }
                    return PutObjectResponse.builder().build();
                });
        R2FileStorageService service = new R2FileStorageService(properties(), s3Client);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "listing.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        );

        StoredFile stored = service.store(file, FileAccessLevel.PUBLIC);

        assertThat(service.provider()).isEqualTo(StorageProvider.R2);
        assertThat(stored.storageKey()).startsWith("public/");
        assertThat(stored.storageKey()).endsWith(".png");
        assertThat(stored.checksumSha256()).isEqualTo(sha256(PNG_BYTES));
        assertThat(stored.publicUrl()).isEqualTo(
                "https://cdn.example.test/" + stored.storageKey()
        );
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void privateStoreShouldNotExposePublicUrl() {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    RequestBody body = invocation.getArgument(1);
                    try (var input = body.contentStreamProvider().newStream()) {
                        input.readAllBytes();
                    }
                    return PutObjectResponse.builder().build();
                });
        R2FileStorageService service = new R2FileStorageService(properties(), s3Client);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 test".getBytes()
        );

        StoredFile stored = service.store(file, FileAccessLevel.PRIVATE);

        assertThat(stored.storageKey()).startsWith("private/");
        assertThat(stored.publicUrl()).isNull();
    }

    @Test
    void storageConfigShouldSelectProviderImplementation() {
        StorageProperties localProperties = new StorageProperties(
                StorageProvider.LOCAL,
                Path.of("./var/storage"),
                "",
                null,
                "auto",
                "",
                "",
                "",
                Duration.ofMinutes(15),
                DataSize.ofMegabytes(10),
                Set.of("image/png")
        );

        assertThat(new StorageConfig(localProperties).fileStorageService(localProperties))
                .isInstanceOf(LocalFileStorageService.class);
        assertThat(new StorageConfig(properties()).fileStorageService(properties()))
                .isInstanceOf(R2FileStorageService.class);
    }

    @Test
    void loadAndDeleteShouldUseS3ObjectKey() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(PNG_BYTES))
        );
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(response);
        R2FileStorageService service = new R2FileStorageService(properties(), s3Client);

        Resource resource = service.load("private/2026/06/file.png");
        service.delete("private/2026/06/file.png");

        assertThat(resource.getInputStream().readAllBytes()).isEqualTo(PNG_BYTES);
        verify(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void createDownloadUrlShouldUseConfiguredSignedUrlTtl() {
        R2FileStorageService service = new R2FileStorageService(properties(), mock(S3Client.class));

        String downloadUrl = service.createDownloadUrl("private/2026/06/file.png");

        assertThat(downloadUrl)
                .contains("https://account-id.r2.cloudflarestorage.com")
                .contains("private/2026/06/file.png")
                .contains("X-Amz-Expires=900");
    }

    @Test
    void missingR2ConfigurationShouldFailFast() {
        assertThatThrownBy(() -> new StorageProperties(
                StorageProvider.R2,
                Path.of("./var/storage"),
                "",
                null,
                "auto",
                "",
                "",
                "",
                Duration.ofMinutes(15),
                DataSize.ofMegabytes(10),
                Set.of("image/png")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.storage.bucket");
    }

    @Test
    void missingObjectsShouldMapToFileUploadException() {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());
        R2FileStorageService service = new R2FileStorageService(properties(), s3Client);

        assertThatThrownBy(() -> service.load("private/missing.png"))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("not found");
    }

    private StorageProperties properties() {
        return new StorageProperties(
                StorageProvider.R2,
                Path.of("./var/storage"),
                "real-estate",
                URI.create("https://account-id.r2.cloudflarestorage.com"),
                "auto",
                "access-key",
                "secret-key",
                "https://cdn.example.test/",
                Duration.ofMinutes(15),
                DataSize.ofMegabytes(10),
                Set.of("image/png", "application/pdf")
        );
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
