package com.javaweb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String fullName;

    public DevAdminSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin.enabled:false}") boolean enabled,
            @Value("${app.seed.admin.email:admin@realestate.local}") String email,
            @Value("${app.seed.admin.password:}") String password,
            @Value("${app.seed.admin.full-name:Development Administrator}") String fullName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        validateConfiguration();

        Integer existingUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );
        if (existingUsers != null && existingUsers > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email, password_hash, full_name, status, email_verified
                ) VALUES (?, ?, ?, 'ACTIVE', TRUE)
                """,
                email,
                passwordEncoder.encode(password),
                fullName
        );
        jdbcTemplate.update(
                """
                INSERT INTO user_roles (user_id, role_id)
                SELECT users.id, roles.id
                FROM users
                JOIN roles ON roles.code = 'ADMIN'
                WHERE users.email = ?
                """,
                email
        );
    }

    private void validateConfiguration() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("DEV_ADMIN_EMAIL must be configured when dev admin seed is enabled");
        }
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH || !isStrongPassword(password)) {
            throw new IllegalStateException(
                    "DEV_ADMIN_PASSWORD must contain at least " + MINIMUM_PASSWORD_LENGTH
                            + " characters, uppercase, lowercase, number, and special character"
            );
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalStateException("DEV_ADMIN_FULL_NAME must be configured when dev admin seed is enabled");
        }
    }

    private boolean isStrongPassword(String value) {
        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isLowerCase(current)) {
                hasLowercase = true;
            } else if (Character.isUpperCase(current)) {
                hasUppercase = true;
            } else if (Character.isDigit(current)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(current)) {
                hasSpecial = true;
            }
        }
        return hasLowercase && hasUppercase && hasDigit && hasSpecial;
    }
}
