package com.javaweb.customer.dto;

import com.javaweb.customer.enums.CustomerPriority;
import com.javaweb.customer.enums.CustomerSource;
import com.javaweb.customer.enums.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String code,
        String fullName,
        String email,
        String phone,
        CustomerStatus status,
        CustomerSource source,
        CustomerPriority priority,
        String preferredContactMethod,
        String notes,
        Long userId,
        String userName,
        Long assignedAgentId,
        String assignedAgentName,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
}
