package com.javaweb.ai.controller;

import com.javaweb.ai.dto.ImageAnalysisRequest;
import com.javaweb.ai.dto.ImageAnalysisResponse;
import com.javaweb.ai.service.ImageAnalysisService;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class AiImageAnalysisController {
    private final ImageAnalysisService imageAnalysisService;

    public AiImageAnalysisController(ImageAnalysisService imageAnalysisService) {
        this.imageAnalysisService = imageAnalysisService;
    }

    @PostMapping("/property-images/analyze")
    public ApiResponse<ImageAnalysisResponse> analyze(
            @Valid @RequestBody ImageAnalysisRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property image analysis generated successfully",
                imageAnalysisService.analyze(request, actor)
        );
    }
}
