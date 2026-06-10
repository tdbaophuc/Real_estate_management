package com.javaweb.transaction.dto;

import com.javaweb.transaction.enums.TransactionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionStatusUpdateRequest(
        @NotNull TransactionStatus status,
        @Size(max = 1000) String reason
) {
    public TransactionStatusUpdateRequest {
        reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    @AssertTrue(message = "reason is required when cancelling a transaction")
    public boolean hasCancellationReason() {
        return status != TransactionStatus.CANCELLED || reason != null;
    }
}
