package com.javaweb.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 1000, message = "reason must not exceed 1000 characters")
        String reason
) {
    public AppointmentCancelRequest {
        reason = reason == null ? null : reason.trim();
    }
}
