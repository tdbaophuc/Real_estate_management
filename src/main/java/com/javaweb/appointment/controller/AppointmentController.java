package com.javaweb.appointment.controller;

import com.javaweb.appointment.dto.AppointmentCancelRequest;
import com.javaweb.appointment.dto.AppointmentCreateRequest;
import com.javaweb.appointment.dto.AppointmentRescheduleRequest;
import com.javaweb.appointment.dto.AppointmentResponse;
import com.javaweb.appointment.dto.AppointmentSearchRequest;
import com.javaweb.appointment.dto.ViewingFeedbackRequest;
import com.javaweb.appointment.dto.ViewingFeedbackResponse;
import com.javaweb.appointment.service.AppointmentService;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/appointments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN')")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentResponse> create(
            @Valid @RequestBody AppointmentCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Appointment created successfully",
                appointmentService.create(request, actor)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ApiResponse<PageResponse<AppointmentResponse>> list(
            @Valid @ParameterObject AppointmentSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(appointmentService.search(request, actor));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<AppointmentResponse>> listMy(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(appointmentService.listMy(page, size, actor));
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<AppointmentResponse> get(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(appointmentService.get(appointmentId, actor));
    }

    @PatchMapping("/{appointmentId}/confirm")
    public ApiResponse<AppointmentResponse> confirm(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Appointment confirmed successfully",
                appointmentService.confirm(appointmentId, actor)
        );
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ApiResponse<AppointmentResponse> cancel(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentCancelRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Appointment cancelled successfully",
                appointmentService.cancel(appointmentId, request, actor)
        );
    }

    @PatchMapping("/{appointmentId}/reschedule")
    public ApiResponse<AppointmentResponse> reschedule(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentRescheduleRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Appointment rescheduled successfully",
                appointmentService.reschedule(appointmentId, request, actor)
        );
    }

    @PatchMapping("/{appointmentId}/complete")
    public ApiResponse<AppointmentResponse> complete(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Appointment completed successfully",
                appointmentService.complete(appointmentId, actor)
        );
    }

    @PostMapping("/{appointmentId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ViewingFeedbackResponse> addFeedback(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ViewingFeedbackRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Viewing feedback created successfully",
                appointmentService.addFeedback(appointmentId, request, actor)
        );
    }
}
