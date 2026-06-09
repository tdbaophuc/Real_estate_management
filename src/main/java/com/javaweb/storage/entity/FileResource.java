package com.javaweb.storage.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "file_resources")
public class FileResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private StorageProvider storageProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private FileAccessLevel accessLevel;

    @Column(name = "public_url", length = 1000)
    private String publicUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FileResource() {
    }

    public FileResource(
            User uploadedBy,
            String originalFileName,
            String storageKey,
            String contentType,
            long fileSize,
            String checksumSha256,
            StorageProvider storageProvider,
            FileAccessLevel accessLevel,
            String publicUrl
    ) {
        this.uploadedBy = uploadedBy;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksumSha256 = checksumSha256;
        this.storageProvider = storageProvider;
        this.accessLevel = accessLevel;
        this.publicUrl = publicUrl;
    }

    public Long getId() {
        return id;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public FileAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
