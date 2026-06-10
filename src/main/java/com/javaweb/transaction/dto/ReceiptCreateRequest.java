package com.javaweb.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReceiptCreateRequest(
        @NotBlank @Size(max = 100) String receiptNumber,
        Instant issuedAt,
        @Size(max = 200) String payerName,
        @Size(max = 1000) String notes
) {
    public ReceiptCreateRequest {
        receiptNumber = receiptNumber == null ? null : receiptNumber.trim().toUpperCase();
        payerName = payerName == null || payerName.isBlank() ? null : payerName.trim();
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
