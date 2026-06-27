package com.javaweb.storage.service;

import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.enums.StorageProvider;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StorageProvider provider();

    StoredFile store(MultipartFile file, FileAccessLevel accessLevel);

    Resource load(String storageKey);

    String createDownloadUrl(String storageKey);

    StoredFile changeAccessLevel(String storageKey, FileAccessLevel accessLevel);

    void delete(String storageKey);
}
