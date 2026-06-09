package com.javaweb.common.response;

public record ValidationError(
        String field,
        String message
) {
}
