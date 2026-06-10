package com.javaweb.listing.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.listing.dto.ListingCreateRequest;
import com.javaweb.listing.dto.ListingResponse;
import com.javaweb.listing.dto.ListingUpdateRequest;
import com.javaweb.listing.dto.RejectListingRequest;
import com.javaweb.listing.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/listings")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ListingResponse> create(
            @Valid @RequestBody ListingCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing created successfully",
                listingService.create(request, actor)
        );
    }

    @PutMapping("/{listingId}")
    public ApiResponse<ListingResponse> update(
            @PathVariable Long listingId,
            @Valid @RequestBody ListingUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing updated successfully",
                listingService.update(listingId, request, actor)
        );
    }

    @PatchMapping("/{listingId}/submit")
    public ApiResponse<ListingResponse> submit(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing submitted for review successfully",
                listingService.submit(listingId, actor)
        );
    }

    @PatchMapping("/{listingId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<ListingResponse> approve(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing approved successfully",
                listingService.approve(listingId, actor)
        );
    }

    @PatchMapping("/{listingId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<ListingResponse> reject(
            @PathVariable Long listingId,
            @Valid @RequestBody RejectListingRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing rejected successfully",
                listingService.reject(listingId, request, actor)
        );
    }

    @PatchMapping("/{listingId}/publish")
    public ApiResponse<ListingResponse> publish(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing published successfully",
                listingService.publish(listingId, actor)
        );
    }

    @PatchMapping("/{listingId}/unpublish")
    public ApiResponse<ListingResponse> unpublish(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Listing unpublished successfully",
                listingService.unpublish(listingId, actor)
        );
    }
}
