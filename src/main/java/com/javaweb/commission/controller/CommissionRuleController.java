package com.javaweb.commission.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.commission.dto.CommissionRuleRequest;
import com.javaweb.commission.dto.CommissionRuleResponse;
import com.javaweb.commission.dto.CommissionRuleSearchRequest;
import com.javaweb.commission.service.CommissionRuleService;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/commission-rules")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class CommissionRuleController {
    private final CommissionRuleService ruleService;

    public CommissionRuleController(CommissionRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommissionRuleResponse> create(
            @Valid @RequestBody CommissionRuleRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Commission rule created successfully",
                ruleService.create(request, actor)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<CommissionRuleResponse>> list(
            @Valid @ParameterObject CommissionRuleSearchRequest request
    ) {
        return ApiResponse.success(ruleService.search(request));
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<CommissionRuleResponse> update(
            @PathVariable Long ruleId,
            @Valid @RequestBody CommissionRuleRequest request
    ) {
        return ApiResponse.success(
                "Commission rule updated successfully",
                ruleService.update(ruleId, request)
        );
    }
}
