package com.javaweb.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiErrorResponse", description = "Standard error response wrapper")
public record ApiErrorResponse(
        @Schema(description = "Always false for error responses", example = "false")
        boolean success,

        @Schema(description = "Stable application-level error code", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "Human-readable error message", example = "Request validation failed")
        String message,

        @Schema(description = "Field-level validation errors. Empty for non-validation failures.")
        List<ValidationError> errors,

        @Schema(description = "Request path that produced the error", example = "/api/v1/properties")
        String path,

        @Schema(description = "Error creation time in UTC", example = "2026-06-12T00:00:00Z")
        Instant timestamp
) {
    public ApiErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        return of(code, message, List.of(), path);
    }

    public static ApiErrorResponse of(
            String code,
            String message,
            List<ValidationError> errors,
            String path
    ) {
        return new ApiErrorResponse(false, code, message, errors, path, Instant.now());
    }
}
