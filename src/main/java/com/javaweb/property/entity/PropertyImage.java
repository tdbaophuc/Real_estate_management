package com.javaweb.property.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.storage.entity.FileResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "property_images")
public class PropertyImage extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_resource_id", unique = true)
    private FileResource fileResource;

    @Column(name = "storage_key", unique = true, length = 500)
    private String storageKey;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "cover_image", nullable = false)
    private boolean coverImage;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected PropertyImage() {
    }

    public PropertyImage(User uploadedBy, FileResource fileResource) {
        this.uploadedBy = uploadedBy;
        this.fileResource = fileResource;
        this.storageKey = fileResource.getStorageKey();
        this.imageUrl = fileResource.getPublicUrl();
        this.fileName = fileResource.getOriginalFileName();
        this.mimeType = fileResource.getContentType();
        this.fileSize = fileResource.getFileSize();
    }

    public PropertyImage(User uploadedBy, String imageUrl) {
        this.uploadedBy = uploadedBy;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    void setProperty(Property property) {
        this.property = property;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public FileResource getFileResource() {
        return fileResource;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public boolean isCoverImage() {
        return coverImage;
    }

    public void setCoverImage(boolean coverImage) {
        this.coverImage = coverImage;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
