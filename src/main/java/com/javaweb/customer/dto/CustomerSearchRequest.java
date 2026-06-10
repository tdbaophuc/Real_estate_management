package com.javaweb.customer.dto;

import com.javaweb.customer.enums.CustomerPriority;
import com.javaweb.customer.enums.CustomerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

public record CustomerSearchRequest(
        @Size(max = 200, message = "keyword must not exceed 200 characters")
        String keyword,
        CustomerStatus status,
        CustomerPriority priority,
        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public CustomerSearchRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;
        direction = direction == null ? Sort.Direction.DESC : direction;
    }
}
