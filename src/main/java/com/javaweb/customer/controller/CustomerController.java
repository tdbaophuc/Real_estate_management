package com.javaweb.customer.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.customer.dto.CustomerDetailResponse;
import com.javaweb.customer.dto.CustomerNotePinRequest;
import com.javaweb.customer.dto.CustomerNoteRequest;
import com.javaweb.customer.dto.CustomerNoteResponse;
import com.javaweb.customer.dto.CustomerRequirementRequest;
import com.javaweb.customer.dto.CustomerRequirementResponse;
import com.javaweb.customer.dto.CustomerResponse;
import com.javaweb.customer.dto.CustomerSearchRequest;
import com.javaweb.customer.dto.CustomerTagRequest;
import com.javaweb.customer.dto.CustomerTagResponse;
import com.javaweb.customer.dto.CustomerTimelineItemResponse;
import com.javaweb.customer.dto.CustomerUpsertRequest;
import com.javaweb.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(
            @Valid @RequestBody CustomerUpsertRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer created successfully",
                customerService.create(request, actor)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @Valid @ParameterObject CustomerSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(customerService.search(request, actor));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<CustomerDetailResponse> get(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(customerService.get(customerId, actor));
    }

    @PutMapping("/{customerId}")
    public ApiResponse<CustomerResponse> update(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerUpsertRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer updated successfully",
                customerService.update(customerId, request, actor)
        );
    }

    @DeleteMapping("/{customerId}")
    public ApiResponse<Void> delete(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        customerService.delete(customerId, actor);
        return ApiResponse.success("Customer deleted successfully", null);
    }

    @PostMapping("/{customerId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerNoteResponse> addNote(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerNoteRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer note created successfully",
                customerService.addNote(customerId, request, actor)
        );
    }

    @PutMapping("/{customerId}/notes/{noteId}")
    @Operation(summary = "Update customer note")
    public ApiResponse<CustomerNoteResponse> updateNote(
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody CustomerNoteRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer note updated successfully",
                customerService.updateNote(customerId, noteId, request, actor)
        );
    }

    @DeleteMapping("/{customerId}/notes/{noteId}")
    @Operation(summary = "Delete customer note")
    public ApiResponse<Void> deleteNote(
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        customerService.deleteNote(customerId, noteId, actor);
        return ApiResponse.success("Customer note deleted successfully", null);
    }

    @PatchMapping("/{customerId}/notes/{noteId}/pin")
    @Operation(summary = "Pin or unpin customer note")
    public ApiResponse<CustomerNoteResponse> pinNote(
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody CustomerNotePinRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer note pin state updated successfully",
                customerService.pinNote(customerId, noteId, request, actor)
        );
    }

    @PostMapping("/{customerId}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerRequirementResponse> addRequirement(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequirementRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer requirement created successfully",
                customerService.addRequirement(customerId, request, actor)
        );
    }

    @PutMapping("/{customerId}/requirements/{requirementId}")
    @Operation(summary = "Update customer requirement")
    public ApiResponse<CustomerRequirementResponse> updateRequirement(
            @PathVariable Long customerId,
            @PathVariable Long requirementId,
            @Valid @RequestBody CustomerRequirementRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer requirement updated successfully",
                customerService.updateRequirement(customerId, requirementId, request, actor)
        );
    }

    @DeleteMapping("/{customerId}/requirements/{requirementId}")
    @Operation(summary = "Deactivate customer requirement")
    public ApiResponse<Void> deleteRequirement(
            @PathVariable Long customerId,
            @PathVariable Long requirementId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        customerService.deleteRequirement(customerId, requirementId, actor);
        return ApiResponse.success("Customer requirement deleted successfully", null);
    }

    @GetMapping("/{customerId}/tags")
    @Operation(summary = "List customer tags")
    public ApiResponse<List<CustomerTagResponse>> listTags(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(customerService.listTags(customerId, actor));
    }

    @PostMapping("/{customerId}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add customer tag")
    public ApiResponse<CustomerTagResponse> addTag(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerTagRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Customer tag created successfully",
                customerService.addTag(customerId, request, actor)
        );
    }

    @DeleteMapping("/{customerId}/tags/{tagId}")
    @Operation(summary = "Delete customer tag")
    public ApiResponse<Void> deleteTag(
            @PathVariable Long customerId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        customerService.deleteTag(customerId, tagId, actor);
        return ApiResponse.success("Customer tag deleted successfully", null);
    }

    @GetMapping("/{customerId}/timeline")
    public ApiResponse<List<CustomerTimelineItemResponse>> timeline(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(customerService.getTimeline(customerId, actor));
    }
}
