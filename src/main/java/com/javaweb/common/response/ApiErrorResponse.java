package com.javaweb.common.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        List<ValidationError> errors,
        String path,
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
