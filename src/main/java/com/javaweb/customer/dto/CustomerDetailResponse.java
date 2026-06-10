package com.javaweb.customer.dto;

import java.util.List;

public record CustomerDetailResponse(
        CustomerResponse customer,
        List<CustomerRequirementResponse> requirements,
        List<CustomerNoteResponse> notes
) {
}
