package com.javaweb.listing.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.service.ListingFavoriteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/listings")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
public class ListingFavoriteController {
    private final ListingFavoriteService favoriteService;

    public ListingFavoriteController(ListingFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{listingId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PublicListingResponse> favorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Listing added to favorites",
                favoriteService.favorite(listingId, principal)
        );
    }

    @DeleteMapping("/{listingId}/favorite")
    public ApiResponse<Void> unfavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        favoriteService.unfavorite(listingId, principal);
        return ApiResponse.success("Listing removed from favorites", null);
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<PublicListingResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(favoriteService.getMyFavorites(principal, page, size));
    }
}
