package com.javaweb.listing.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.listing.dto.ListingSearchRequest;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.service.PublicListingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/search/listings")
public class PublicListingController {
    private final PublicListingService publicListingService;

    public PublicListingController(PublicListingService publicListingService) {
        this.publicListingService = publicListingService;
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
}
