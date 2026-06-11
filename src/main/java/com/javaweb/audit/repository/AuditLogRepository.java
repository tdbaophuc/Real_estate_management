package com.javaweb.audit.repository;

import com.javaweb.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findAllByActionAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String action,
            String resourceType,
            Long resourceId
    );
}
