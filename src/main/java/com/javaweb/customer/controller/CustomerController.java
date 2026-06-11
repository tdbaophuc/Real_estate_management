package com.javaweb.customer.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.customer.dto.CustomerDetailResponse;
import com.javaweb.customer.dto.CustomerNoteRequest;
import com.javaweb.customer.dto.CustomerNoteResponse;
import com.javaweb.customer.dto.CustomerRequirementRequest;
import com.javaweb.customer.dto.CustomerRequirementResponse;
import com.javaweb.customer.dto.CustomerResponse;
import com.javaweb.customer.dto.CustomerSearchRequest;
import com.javaweb.customer.dto.CustomerTimelineItemResponse;
import com.javaweb.customer.dto.CustomerUpsertRequest;
import com.javaweb.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/{customerId}/timeline")
    public ApiResponse<List<CustomerTimelineItemResponse>> timeline(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(customerService.getTimeline(customerId, actor));
    }
}
