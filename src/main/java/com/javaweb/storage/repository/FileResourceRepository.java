package com.javaweb.storage.repository;

import com.javaweb.storage.entity.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileResourceRepository extends JpaRepository<FileResource, Long> {
}
