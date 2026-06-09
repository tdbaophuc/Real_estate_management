package com.javaweb.auth.controller;

import com.javaweb.auth.dto.AssignUserRolesRequest;
import com.javaweb.auth.dto.UpdateUserStatusRequest;
import com.javaweb.auth.dto.UserManagementResponse;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.service.UserManagementService;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class UserManagementController {
    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserManagementResponse>> listUsers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be at least 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        return ApiResponse.success(
                userManagementService.listUsers(page, size, sortBy, direction)
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserManagementResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userManagementService.getUser(userId));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserManagementResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "User status updated successfully",
                userManagementService.updateStatus(userId, request, actor)
        );
    }

    @PutMapping("/{userId}/roles")
    public ApiResponse<UserManagementResponse> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignUserRolesRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "User roles updated successfully",
                userManagementService.assignRoles(userId, request, actor)
        );
    }
}
