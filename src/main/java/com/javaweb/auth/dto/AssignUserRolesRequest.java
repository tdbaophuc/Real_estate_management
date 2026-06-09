package com.javaweb.auth.dto;

import com.javaweb.auth.enums.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AssignUserRolesRequest(
        @NotEmpty(message = "roles must contain at least one role")
        @Size(max = 5, message = "roles must not contain more than 5 roles")
        Set<@NotNull(message = "role must not be null") RoleCode> roles
) {
    public AssignUserRolesRequest {
        roles = roles == null ? null : Set.copyOf(roles);
    }
}
