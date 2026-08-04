package com.javaweb.auth.dto;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.security.AuthUserPrincipal;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        List<String> roles,
        List<String> permissions,
        String avatarUrl
) {
    public static AuthUserResponse from(AuthUserPrincipal principal) {
        return new AuthUserResponse(
                principal.id(),
                principal.getUsername(),
                principal.fullName(),
                principal.phone(),
                principal.status(),
                principal.roles(),
                principal.permissions(),
                principal.avatarUrl()
        );
    }

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                user.getRoles().stream()
                        .map(role -> role.getCode().name())
                        .sorted()
                        .toList(),
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getCode())
                        .distinct()
                        .sorted()
                        .toList(),
                user.getAvatarFileResource() == null
                        ? null
                        : user.getAvatarFileResource().getPublicUrl()
        );
    }
}
