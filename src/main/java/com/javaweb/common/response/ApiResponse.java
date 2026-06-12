package com.javaweb.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ApiResponse", description = "Standard success response wrapper")
public record ApiResponse<T>(
        @Schema(description = "Always true for successful responses", example = "true")
        boolean success,

        @Schema(description = "Stable application-level success code", example = "SUCCESS")
        String code,

        @Schema(description = "Human-readable success message", example = "Request processed successfully")
        String message,

        @Schema(description = "Endpoint-specific response payload")
        T data,

        @Schema(description = "Response creation time in UTC", example = "2026-06-12T00:00:00Z")
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return success("Request processed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, Instant.now());
    }
}
