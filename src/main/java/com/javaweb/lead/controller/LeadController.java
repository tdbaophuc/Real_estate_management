package com.javaweb.lead.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.lead.dto.FollowUpTaskRequest;
import com.javaweb.lead.dto.FollowUpTaskResponse;
import com.javaweb.lead.dto.LeadActivityRequest;
import com.javaweb.lead.dto.LeadActivityResponse;
import com.javaweb.lead.dto.LeadAssignRequest;
import com.javaweb.lead.dto.LeadAssignmentResponse;
import com.javaweb.lead.dto.LeadCreateRequest;
import com.javaweb.lead.dto.LeadDetailResponse;
import com.javaweb.lead.dto.LeadNoteRequest;
import com.javaweb.lead.dto.LeadNoteResponse;
import com.javaweb.lead.dto.LeadResponse;
import com.javaweb.lead.dto.LeadSearchRequest;
import com.javaweb.lead.dto.LeadStatusUpdateRequest;
import com.javaweb.lead.service.LeadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/leads")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class LeadController {
    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeadResponse> create(
            @Valid @RequestBody LeadCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Lead created successfully",
                leadService.create(request, actor)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<LeadResponse>> list(
            @Valid @ParameterObject LeadSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(leadService.search(request, actor));
    }

    @GetMapping("/{leadId}")
    public ApiResponse<LeadDetailResponse> get(
            @PathVariable Long leadId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(leadService.get(leadId, actor));
    }

    @PatchMapping("/{leadId}/assign")
    public ApiResponse<LeadAssignmentResponse> assign(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadAssignRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Lead assigned successfully",
                leadService.assign(leadId, request, actor)
        );
    }

    @PatchMapping("/{leadId}/status")
    public ApiResponse<LeadResponse> updateStatus(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadStatusUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Lead status updated successfully",
                leadService.updateStatus(leadId, request, actor)
        );
    }

    @PostMapping("/{leadId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeadNoteResponse> addNote(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadNoteRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Lead note created successfully",
                leadService.addNote(leadId, request, actor)
        );
    }

    @PostMapping("/{leadId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeadActivityResponse> addActivity(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadActivityRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Lead activity created successfully",
                leadService.addActivity(leadId, request, actor)
        );
    }

    @PostMapping("/{leadId}/follow-up-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FollowUpTaskResponse> createFollowUpTask(
            @PathVariable Long leadId,
            @Valid @RequestBody FollowUpTaskRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Follow-up task created successfully",
                leadService.createFollowUpTask(leadId, request, actor)
        );
    }
}
