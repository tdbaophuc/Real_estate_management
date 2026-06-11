package com.javaweb.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dev_admin_seed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.seed.admin.enabled=true",
        "app.seed.admin.email=dev-admin@example.test",
        "app.seed.admin.password=StrongDevPassword123!",
        "app.seed.admin.full-name=Dev Administrator"
})
@ActiveProfiles("dev")
class DevAdminSeederIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DevAdminSeeder devAdminSeeder;

    @Test
    void shouldSeedDevAdminWithHashedPasswordAndAdminRole() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                "dev-admin@example.test"
        );

        assertThat(passwordHash).isNotEqualTo("StrongDevPassword123!");
        assertThat(new BCryptPasswordEncoder().matches("StrongDevPassword123!", passwordHash)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_roles
                JOIN users ON users.id = user_roles.user_id
                JOIN roles ON roles.id = user_roles.role_id
                WHERE users.email = ? AND roles.code = 'ADMIN'
                """,
                Integer.class,
                "dev-admin@example.test"
        )).isEqualTo(1);

        devAdminSeeder.run(new DefaultApplicationArguments());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                "dev-admin@example.test"
        )).isEqualTo(1);
    }
}
