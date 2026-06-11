package com.javaweb.transaction.dto;

import com.javaweb.contract.enums.ContractType;
import com.javaweb.transaction.enums.TransactionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;

public record TransactionSearchRequest(
        TransactionStatus status,
        ContractType transactionType,
        @Positive Long propertyId,
        @Positive Long customerId,
        @Positive Long agentId,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public TransactionSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        direction = direction == null ? Sort.Direction.DESC : direction;
    }
}
