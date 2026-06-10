package com.javaweb.storage.service;

import com.javaweb.storage.enums.FileAccessLevel;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, FileAccessLevel accessLevel);

    Resource load(String storageKey);

    void delete(String storageKey);
}
