package com.javaweb.appointment.dto;

import com.javaweb.appointment.enums.AppointmentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

import java.time.Instant;

public record AppointmentSearchRequest(
        AppointmentStatus status,
        @Positive(message = "agentId must be positive")
        Long agentId,
        @Positive(message = "customerId must be positive")
        Long customerId,
        @Positive(message = "propertyId must be positive")
        Long propertyId,
        Instant from,
        Instant to,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public AppointmentSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "startAt" : sortBy;
        direction = direction == null ? Sort.Direction.ASC : direction;
    }

    @AssertTrue(message = "to must be after from")
    public boolean hasValidRange() {
        return from == null || to == null || to.isAfter(from);
    }
}
