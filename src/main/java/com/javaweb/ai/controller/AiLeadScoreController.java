package com.javaweb.ai.controller;

import com.javaweb.ai.dto.LeadScoreRequest;
import com.javaweb.ai.dto.LeadScoreResponse;
import com.javaweb.ai.service.LeadScoreService;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class AiLeadScoreController {
    private final LeadScoreService leadScoreService;

    public AiLeadScoreController(LeadScoreService leadScoreService) {
        this.leadScoreService = leadScoreService;
    }

    @PostMapping("/leads/{leadId}/score")
    public ApiResponse<LeadScoreResponse> score(
            @PathVariable Long leadId,
            @Valid @RequestBody(required = false) LeadScoreRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        LeadScoreRequest safeRequest = request == null ? new LeadScoreRequest(null) : request;
        return ApiResponse.success(
                "Lead score generated successfully",
                leadScoreService.score(leadId, safeRequest, actor)
        );
    }
}
