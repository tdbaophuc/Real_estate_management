package com.javaweb.commission.dto;

import com.javaweb.commission.enums.CommissionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

public record CommissionSearchRequest(
        CommissionStatus status,
        @Positive Long transactionId,
        @Positive Long beneficiaryUserId,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public CommissionSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        direction = direction == null ? Sort.Direction.DESC : direction;
    }
}
