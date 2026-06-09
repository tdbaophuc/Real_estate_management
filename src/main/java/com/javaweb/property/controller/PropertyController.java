package com.javaweb.property.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.property.dto.PropertyResponse;
import com.javaweb.property.dto.PropertyUpsertRequest;
import com.javaweb.property.dto.UpdatePropertyStatusRequest;
import com.javaweb.property.service.PropertyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/properties")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class PropertyController {
    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PropertyResponse>> list(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be at least 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(propertyService.list(page, size, sortBy, direction, actor));
    }

    @GetMapping("/{propertyId}")
    public ApiResponse<PropertyResponse> get(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(propertyService.get(propertyId, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PropertyResponse> create(
            @Valid @RequestBody PropertyUpsertRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property created successfully",
                propertyService.create(request, actor)
        );
    }

    @PutMapping("/{propertyId}")
    public ApiResponse<PropertyResponse> update(
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyUpsertRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property updated successfully",
                propertyService.update(propertyId, request, actor)
        );
    }

    @DeleteMapping("/{propertyId}")
    public ApiResponse<Void> delete(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        propertyService.delete(propertyId, actor);
        return ApiResponse.success("Property deleted successfully", null);
    }

    @PatchMapping("/{propertyId}/status")
    public ApiResponse<PropertyResponse> updateStatus(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePropertyStatusRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property status updated successfully",
                propertyService.updateStatus(propertyId, request, actor)
        );
    }
}
