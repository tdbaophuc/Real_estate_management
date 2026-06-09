package com.javaweb.auth.repository;

import com.javaweb.auth.entity.OtpToken;
import com.javaweb.auth.entity.Permission;
import com.javaweb.auth.entity.RefreshToken;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.OtpTokenType;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuthRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Test
    void shouldPersistAndLoadUserWithRoleAndPermission() {
        Permission permission = permissionRepository.findByCode("USER_READ").orElseThrow();
        Role role = roleRepository.findByCode(RoleCode.MANAGER).orElseThrow();
        role.addPermission(permission);
        roleRepository.saveAndFlush(role);

        User user = new User("agent@example.test", "bcrypt-hash", "Test Agent");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        userRepository.saveAndFlush(user);

        User loaded = userRepository.findWithRolesByEmailIgnoreCase("AGENT@example.test").orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(loaded.getRoles()).extracting(Role::getCode).contains(RoleCode.MANAGER);
        assertThat(loaded.getRoles())
                .flatExtracting(Role::getPermissions)
                .extracting(Permission::getCode)
                .contains("USER_READ");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldPersistAndQueryRefreshAndOtpTokens() {
        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER).orElseThrow();
        User user = new User("customer@example.test", "bcrypt-hash", "Test Customer");
        user.addRole(customerRole);
        user = userRepository.saveAndFlush(user);

        Instant now = Instant.now();
        RefreshToken refreshToken = refreshTokenRepository.saveAndFlush(
                new RefreshToken(user, "refresh-token-hash", now.plusSeconds(3600))
        );
        OtpToken otpToken = otpTokenRepository.saveAndFlush(
                new OtpToken(
                        user,
                        "otp-token-hash",
                        OtpTokenType.EMAIL_VERIFICATION,
                        now.plusSeconds(600)
                )
        );

        assertThat(refreshTokenRepository.findByTokenHash("refresh-token-hash"))
                .contains(refreshToken);
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId()))
                .containsExactly(refreshToken);
        assertThat(refreshToken.isExpired(now)).isFalse();
        assertThat(refreshToken.isRevoked()).isFalse();

        assertThat(otpTokenRepository
                .findFirstByUserIdAndTokenTypeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId(),
                        OtpTokenType.EMAIL_VERIFICATION
                ))
                .contains(otpToken);
        assertThat(otpToken.getFailedAttempts()).isZero();
        assertThat(otpToken.isConsumed()).isFalse();
        assertThat(otpToken.isExpired(now)).isFalse();
    }
}
