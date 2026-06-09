package com.javaweb.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        AuthUserResponse user
) {
    public static LoginResponse bearer(
            String accessToken,
            long expiresIn,
            String refreshToken,
            long refreshExpiresIn,
            AuthUserResponse user
    ) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                refreshToken,
                refreshExpiresIn,
                user
        );
    }
}
