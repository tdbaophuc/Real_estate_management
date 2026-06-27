package com.javaweb.masterdata.controller;

import com.javaweb.common.response.ApiResponse;
import com.javaweb.masterdata.dto.AmenityOptionResponse;
import com.javaweb.masterdata.dto.LeadSourceOptionResponse;
import com.javaweb.masterdata.dto.ListingPackageOptionResponse;
import com.javaweb.masterdata.dto.LocationOptionResponse;
import com.javaweb.masterdata.dto.PropertyTypeOptionResponse;
import com.javaweb.masterdata.service.MasterDataService;
import com.javaweb.property.enums.AmenityCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master-data")
public class MasterDataController {
    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @Operation(summary = "List active provinces", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/provinces")
    public ApiResponse<List<LocationOptionResponse>> provinces() {
        return ApiResponse.success(masterDataService.provinces());
    }

    @Operation(summary = "List active districts by province", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/provinces/{provinceId}/districts")
    public ApiResponse<List<LocationOptionResponse>> districts(
            @PathVariable Long provinceId
    ) {
        return ApiResponse.success(masterDataService.districts(provinceId));
    }

    @Operation(summary = "List active wards by district", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/districts/{districtId}/wards")
    public ApiResponse<List<LocationOptionResponse>> wards(
            @PathVariable Long districtId
    ) {
        return ApiResponse.success(masterDataService.wards(districtId));
    }

    @Operation(summary = "List active property types", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/property-types")
    public ApiResponse<List<PropertyTypeOptionResponse>> propertyTypes() {
        return ApiResponse.success(masterDataService.propertyTypes());
    }

    @Operation(summary = "List active amenities with optional category filter", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/amenities")
    public ApiResponse<List<AmenityOptionResponse>> amenities(
            @RequestParam(required = false) AmenityCategory category
    ) {
        return ApiResponse.success(masterDataService.amenities(category));
    }

    @Operation(summary = "List active listing packages", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/listing-packages")
    public ApiResponse<List<ListingPackageOptionResponse>> listingPackages() {
        return ApiResponse.success(masterDataService.listingPackages());
    }

    @Operation(summary = "List active lead sources", security = {})
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/lead-sources")
    public ApiResponse<List<LeadSourceOptionResponse>> leadSources() {
        return ApiResponse.success(masterDataService.leadSources());
    }
}
