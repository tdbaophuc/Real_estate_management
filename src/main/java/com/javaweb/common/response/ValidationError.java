package com.javaweb.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationError", description = "Field-level validation error")
public record ValidationError(
        @Schema(description = "Field or parameter that failed validation", example = "email")
        String field,

        @Schema(description = "Validation failure message", example = "must be a well-formed email address")
        String message
) {
}
