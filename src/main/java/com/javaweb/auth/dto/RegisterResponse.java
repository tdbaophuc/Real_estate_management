package com.javaweb.auth.dto;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;

import java.util.List;

public record RegisterResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        List<String> roles
) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                user.getRoles().stream()
                        .map(Role::getCode)
                        .map(Enum::name)
                        .sorted()
                        .toList()
        );
    }
}
