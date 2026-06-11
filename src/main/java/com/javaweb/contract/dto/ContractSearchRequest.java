package com.javaweb.contract.dto;

import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

public record ContractSearchRequest(
        ContractStatus status,
        ContractType contractType,
        @Positive(message = "propertyId must be positive")
        Long propertyId,
        @Positive(message = "customerId must be positive")
        Long customerId,
        @Positive(message = "agentId must be positive")
        Long agentId,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public ContractSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        direction = direction == null ? Sort.Direction.DESC : direction;
    }
}
