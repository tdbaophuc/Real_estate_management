package com.javaweb.contract.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.contract.dto.ContractCancelRequest;
import com.javaweb.contract.dto.ContractCreateRequest;
import com.javaweb.contract.dto.ContractDocumentResponse;
import com.javaweb.contract.dto.ContractResponse;
import com.javaweb.contract.dto.ContractSearchRequest;
import com.javaweb.contract.dto.ContractUpdateRequest;
import com.javaweb.contract.enums.ContractDocumentType;
import com.javaweb.contract.service.ContractService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/contracts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContractResponse> create(
            @Valid @RequestBody ContractCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract created successfully",
                contractService.create(request, actor)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<ContractResponse>> list(
            @Valid @ParameterObject ContractSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(contractService.search(request, actor));
    }

    @GetMapping("/{contractId}")
    public ApiResponse<ContractResponse> get(
            @PathVariable Long contractId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(contractService.get(contractId, actor));
    }

    @PutMapping("/{contractId}")
    public ApiResponse<ContractResponse> update(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract updated successfully",
                contractService.update(contractId, request, actor)
        );
    }

    @PostMapping(
            value = "/{contractId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContractDocumentResponse> uploadDocument(
            @PathVariable Long contractId,
            @RequestParam MultipartFile file,
            @RequestParam ContractDocumentType documentType,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "false") boolean primaryDocument,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract document uploaded successfully",
                contractService.uploadDocument(
                        contractId,
                        file,
                        documentType,
                        displayName,
                        description,
                        primaryDocument,
                        actor
                )
        );
    }

    @PatchMapping("/{contractId}/submit-review")
    public ApiResponse<ContractResponse> submitReview(
            @PathVariable Long contractId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract submitted for review successfully",
                contractService.submitReview(contractId, actor)
        );
    }

    @PatchMapping("/{contractId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<ContractResponse> approve(
            @PathVariable Long contractId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract approved successfully",
                contractService.approve(contractId, actor)
        );
    }

    @PatchMapping("/{contractId}/mark-signed")
    public ApiResponse<ContractResponse> markSigned(
            @PathVariable Long contractId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract marked signed successfully",
                contractService.markSigned(contractId, actor)
        );
    }

    @PatchMapping("/{contractId}/cancel")
    public ApiResponse<ContractResponse> cancel(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractCancelRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Contract cancelled successfully",
                contractService.cancel(contractId, request, actor)
        );
    }
}
