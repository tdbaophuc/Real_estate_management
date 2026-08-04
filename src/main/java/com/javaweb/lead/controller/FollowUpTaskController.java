package com.javaweb.lead.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.lead.dto.FollowUpTaskResponse;
import com.javaweb.lead.dto.FollowUpTaskSearchRequest;
import com.javaweb.lead.dto.FollowUpTaskStatusRequest;
import com.javaweb.lead.dto.FollowUpTaskUpdateRequest;
import com.javaweb.lead.service.FollowUpTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/follow-up-tasks")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class FollowUpTaskController {
    private final FollowUpTaskService followUpTaskService;

    public FollowUpTaskController(FollowUpTaskService followUpTaskService) {
        this.followUpTaskService = followUpTaskService;
    }

    @GetMapping
    @Operation(summary = "Search follow-up tasks")
    public ApiResponse<PageResponse<FollowUpTaskResponse>> list(
            @Valid @ParameterObject FollowUpTaskSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(followUpTaskService.search(request, actor));
    }

    @GetMapping("/my")
    @Operation(summary = "Search my follow-up tasks")
    public ApiResponse<PageResponse<FollowUpTaskResponse>> listMine(
            @Valid @ParameterObject FollowUpTaskSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(followUpTaskService.searchMine(request, actor));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get follow-up task detail")
    public ApiResponse<FollowUpTaskResponse> get(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(followUpTaskService.get(taskId, actor));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update follow-up task")
    public ApiResponse<FollowUpTaskResponse> update(
            @PathVariable Long taskId,
            @Valid @RequestBody FollowUpTaskUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Follow-up task updated successfully",
                followUpTaskService.update(taskId, request, actor)
        );
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update follow-up task status")
    public ApiResponse<FollowUpTaskResponse> updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody FollowUpTaskStatusRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Follow-up task status updated successfully",
                followUpTaskService.updateStatus(taskId, request, actor)
        );
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Cancel follow-up task")
    public ApiResponse<Void> cancel(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        followUpTaskService.cancel(taskId, actor);
        return ApiResponse.success("Follow-up task cancelled successfully", null);
    }
}
