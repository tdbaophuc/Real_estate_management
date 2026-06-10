package com.javaweb.commission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CommissionMarkPaidRequest(
        @NotBlank @Size(max = 100) String paymentReference,
        Instant paidAt,
        @Size(max = 1000) String notes
) {
    public CommissionMarkPaidRequest {
        paymentReference = paymentReference == null
                ? null
                : paymentReference.trim();
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
