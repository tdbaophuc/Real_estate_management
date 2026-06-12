package com.javaweb.ai.controller;

import com.javaweb.ai.dto.CustomerSummaryResponse;
import com.javaweb.ai.service.CustomerSummaryService;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class AiCustomerSummaryController {
    private final CustomerSummaryService customerSummaryService;

    public AiCustomerSummaryController(CustomerSummaryService customerSummaryService) {
        this.customerSummaryService = customerSummaryService;
    }

    @GetMapping("/customers/{customerId}/summary")
    public ApiResponse<CustomerSummaryResponse> summarize(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer summary generated successfully",
                customerSummaryService.summarize(customerId, actor)
        );
    }
}
