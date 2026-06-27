package com.javaweb.storage.repository;

import com.javaweb.storage.entity.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileResourceRepository extends JpaRepository<FileResource, Long> {
    Optional<FileResource> findByStorageKey(String storageKey);
}
