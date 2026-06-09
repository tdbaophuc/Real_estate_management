package com.javaweb.auth.dto;

import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.security.AuthUserPrincipal;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        UserStatus status,
        List<String> roles,
        List<String> permissions
) {
    public static AuthUserResponse from(AuthUserPrincipal principal) {
        return new AuthUserResponse(
                principal.id(),
                principal.getUsername(),
                principal.fullName(),
                principal.status(),
                principal.roles(),
                principal.permissions()
        );
    }
}
