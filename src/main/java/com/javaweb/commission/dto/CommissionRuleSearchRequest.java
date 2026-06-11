package com.javaweb.commission.dto;

import com.javaweb.contract.enums.ContractType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;

public record CommissionRuleSearchRequest(
        Boolean active,
        ContractType transactionType,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public CommissionRuleSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "priority" : sortBy.trim();
        direction = direction == null ? Sort.Direction.DESC : direction;
    }
}
