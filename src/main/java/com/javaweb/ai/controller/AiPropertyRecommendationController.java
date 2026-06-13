package com.javaweb.ai.controller;

import com.javaweb.ai.dto.PropertyRecommendationRequest;
import com.javaweb.ai.dto.PropertyRecommendationResponse;
import com.javaweb.ai.service.PropertyRecommendationService;
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
public class AiPropertyRecommendationController {
    private final PropertyRecommendationService recommendationService;

    public AiPropertyRecommendationController(PropertyRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/customers/{customerId}/recommendations")
    public ApiResponse<PropertyRecommendationResponse> recommend(
            @PathVariable Long customerId,
            @Valid @RequestBody(required = false) PropertyRecommendationRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        PropertyRecommendationRequest safeRequest = request == null
                ? new PropertyRecommendationRequest(null, null, null, null)
                : request;
        return ApiResponse.success(
                "Property recommendations generated successfully",
                recommendationService.recommend(customerId, safeRequest, actor)
        );
    }
}
