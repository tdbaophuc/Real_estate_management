package com.javaweb.listing.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.appointment.dto.AppointmentResponse;
import com.javaweb.lead.dto.LeadResponse;
import com.javaweb.listing.dto.ListingAppointmentRequest;
import com.javaweb.listing.dto.ListingInquiryRequest;
import com.javaweb.listing.dto.ListingSearchRequest;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.service.PublicListingInteractionService;
import com.javaweb.listing.service.PublicListingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/search/listings")
public class PublicListingController {
    private final PublicListingService publicListingService;
    private final PublicListingInteractionService interactionService;

    public PublicListingController(
            PublicListingService publicListingService,
            PublicListingInteractionService interactionService
    ) {
        this.publicListingService = publicListingService;
        this.interactionService = interactionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PublicListingResponse>> search(
            @Valid @ParameterObject ListingSearchRequest request
    ) {
        return ApiResponse.success(publicListingService.search(request));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicListingResponse> get(
            @PathVariable String slug,
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = "X-Session-Id", required = false) String sessionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(publicListingService.getAndRecordView(
                slug,
                principal,
                sessionId,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        ));
    }

    @PostMapping("/{listingId}/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create listing inquiry",
            description = "Public or authenticated endpoint that creates or merges a CRM customer and creates a listing inquiry lead assigned to the listing/property agent. Rate limited by public listing submission limits."
    )
    public ApiResponse<LeadResponse> createInquiry(
            @PathVariable Long listingId,
            @Valid @RequestBody ListingInquiryRequest request,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Listing inquiry created successfully",
                interactionService.createInquiry(listingId, request, principal)
        );
    }

    @PostMapping("/{listingId}/appointment-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create listing appointment request",
            description = "Public or authenticated endpoint that creates or merges a CRM customer, creates a listing inquiry lead, creates a pending appointment, and notifies the assigned listing/property agent. Rate limited by public listing submission limits."
    )
    public ApiResponse<AppointmentResponse> createAppointmentRequest(
            @PathVariable Long listingId,
            @Valid @RequestBody ListingAppointmentRequest request,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Listing appointment request created successfully",
                interactionService.createAppointmentRequest(listingId, request, principal)
        );
    }
}
