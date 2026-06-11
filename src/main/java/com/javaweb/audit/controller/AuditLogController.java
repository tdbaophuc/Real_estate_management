package com.javaweb.audit.controller;

import com.javaweb.audit.dto.AuditLogResponse;
import com.javaweb.audit.dto.AuditLogSearchRequest;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @Valid @ParameterObject AuditLogSearchRequest request
    ) {
        return ApiResponse.success(auditLogService.search(request));
    }

    @GetMapping("/{auditLogId}")
    public ApiResponse<AuditLogResponse> get(@PathVariable Long auditLogId) {
        return ApiResponse.success(auditLogService.get(auditLogId));
    }
}
