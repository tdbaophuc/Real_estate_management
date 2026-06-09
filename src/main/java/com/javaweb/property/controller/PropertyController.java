package com.javaweb.property.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.property.dto.PropertyResponse;
import com.javaweb.property.dto.PropertyImageResponse;
import com.javaweb.property.dto.PropertySearchRequest;
import com.javaweb.property.dto.PropertyUpsertRequest;
import com.javaweb.property.dto.UpdatePropertyStatusRequest;
import com.javaweb.property.service.PropertyService;
import com.javaweb.property.service.PropertyImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/properties")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class PropertyController {
    private final PropertyService propertyService;
    private final PropertyImageService propertyImageService;

    public PropertyController(
            PropertyService propertyService,
            PropertyImageService propertyImageService
    ) {
        this.propertyService = propertyService;
        this.propertyImageService = propertyImageService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PropertyResponse>> list(
            @Valid @ParameterObject PropertySearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(propertyService.search(request, actor));
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

    @GetMapping("/{propertyId}/images")
    public ApiResponse<List<PropertyImageResponse>> listImages(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(propertyImageService.list(propertyId, actor));
    }

    @PostMapping(
            value = "/{propertyId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PropertyImageResponse> uploadImage(
            @PathVariable Long propertyId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "0") @Min(
                    value = 0,
                    message = "displayOrder must be at least 0"
            )
            int displayOrder,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property image uploaded successfully",
                propertyImageService.upload(propertyId, file, altText, displayOrder, actor)
        );
    }

    @DeleteMapping("/{propertyId}/images/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        propertyImageService.delete(propertyId, imageId, actor);
        return ApiResponse.success("Property image deleted successfully", null);
    }

    @PatchMapping("/{propertyId}/cover-image/{imageId}")
    public ApiResponse<PropertyImageResponse> setCoverImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Property cover image updated successfully",
                propertyImageService.setCover(propertyId, imageId, actor)
        );
    }
}
