package com.javaweb.auth.security;

import com.javaweb.auth.entity.Permission;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record AuthUserPrincipal(
        Long id,
        String email,
        String password,
        String fullName,
        UserStatus status,
        Instant lockedUntil,
        List<String> roles,
        List<String> permissions,
        List<GrantedAuthority> authorities
) implements UserDetails {

    public static AuthUserPrincipal from(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .map(Enum::name)
                .sorted()
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
        List<GrantedAuthority> authorities = Stream.concat(
                        roles.stream().map(role -> "ROLE_" + role),
                        permissions.stream()
                )
                .distinct()
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new AuthUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getStatus(),
                user.getLockedUntil(),
                roles,
                permissions,
                authorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED
                && (lockedUntil == null || lockedUntil.isBefore(Instant.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
