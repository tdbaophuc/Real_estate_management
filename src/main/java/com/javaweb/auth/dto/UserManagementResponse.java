package com.javaweb.auth.dto;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;

import java.time.Instant;
import java.util.List;

public record UserManagementResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        boolean emailVerified,
        Instant lockedUntil,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> roles
) {
    public static UserManagementResponse from(User user) {
        return new UserManagementResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream()
                        .map(Role::getCode)
                        .map(Enum::name)
                        .sorted()
                        .toList()
        );
    }
}
