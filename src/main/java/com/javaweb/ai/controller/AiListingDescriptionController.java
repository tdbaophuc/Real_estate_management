package com.javaweb.ai.controller;

import com.javaweb.ai.dto.ListingDescriptionRequest;
import com.javaweb.ai.dto.ListingDescriptionResponse;
import com.javaweb.ai.service.ListingDescriptionService;
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
public class AiListingDescriptionController {
    private final ListingDescriptionService listingDescriptionService;

    public AiListingDescriptionController(ListingDescriptionService listingDescriptionService) {
        this.listingDescriptionService = listingDescriptionService;
    }

    @PostMapping("/listing-description")
    public ApiResponse<ListingDescriptionResponse> generate(
            @Valid @RequestBody ListingDescriptionRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing description generated successfully",
                listingDescriptionService.generate(request, actor)
        );
    }
}
