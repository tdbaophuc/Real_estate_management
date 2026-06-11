package com.javaweb.commission.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.commission.dto.CommissionMarkPaidRequest;
import com.javaweb.commission.dto.CommissionResponse;
import com.javaweb.commission.dto.CommissionSearchRequest;
import com.javaweb.commission.service.CommissionService;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/commissions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class CommissionController {
    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<CommissionResponse>> listMine(
            @Valid @ParameterObject CommissionSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(commissionService.searchMine(request, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<PageResponse<CommissionResponse>> list(
            @Valid @ParameterObject CommissionSearchRequest request
    ) {
        return ApiResponse.success(commissionService.search(request));
    }

    @PatchMapping("/{commissionId}/mark-paid")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<CommissionResponse> markPaid(
            @PathVariable Long commissionId,
            @Valid @RequestBody CommissionMarkPaidRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Commission marked paid successfully",
                commissionService.markPaid(commissionId, request, actor)
        );
    }
}
