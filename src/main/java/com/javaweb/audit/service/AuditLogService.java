package com.javaweb.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.audit.dto.AuditLogResponse;
import com.javaweb.audit.dto.AuditLogSearchRequest;
import com.javaweb.audit.entity.AuditLog;
import com.javaweb.audit.repository.AuditLogRepository;
import com.javaweb.audit.repository.AuditLogSpecifications;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class AuditLogService {
    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "action", "resourceType", "resourceId");

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    public void record(
            AuthUserPrincipal actor,
            String action,
            String resourceType,
            Long resourceId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue
    ) {
        User actorReference = entityManager.getReference(User.class, actor.id());
        auditLogRepository.save(new AuditLog(
                actorReference,
                action,
                resourceType,
                resourceId,
                toJson(oldValue),
                toJson(newValue)
        ));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditLogSearchRequest request) {
        validateRange(request);
        if (!SORT_FIELDS.contains(request.sortBy())) {
            throw new BusinessException("Unsupported audit log sort field");
        }
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.search(request),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), request.sortBy())
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(this::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public AuditLogResponse get(Long auditLogId) {
        return toResponse(auditLogRepository.findById(auditLogId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Audit log not found"
                )));
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        return new AuditLogResponse(
                auditLog.getId(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getEmail(),
                actor == null ? null : actor.getFullName(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                readJson(auditLog.getOldValueJson()),
                readJson(auditLog.getNewValueJson()),
                auditLog.getCreatedAt()
        );
    }

    private String toJson(Map<String, ?> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit value", exception);
        }
    }

    private JsonNode readJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize audit value", exception);
        }
    }

    private void validateRange(AuditLogSearchRequest request) {
        if (request.from() != null
                && request.to() != null
                && request.from().isAfter(request.to())) {
            throw new BusinessException("from must not be after to");
        }
    }
}
