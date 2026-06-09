package com.javaweb.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyDatabaseMigrationsAndSeedMasterData() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getDescription())
                .isEqualTo("create property schema and seed master data");

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT LOWER(table_name)
                FROM information_schema.tables
                WHERE table_schema = 'PUBLIC'
                """,
                String.class
        );

        assertThat(tables).contains(
                "users",
                "roles",
                "permissions",
                "user_roles",
                "role_permissions",
                "refresh_tokens",
                "otp_tokens",
                "provinces",
                "districts",
                "wards",
                "addresses",
                "property_types",
                "amenities",
                "properties",
                "property_amenities",
                "property_images",
                "property_legal_documents",
                "flyway_schema_history"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM roles ORDER BY code",
                String.class
        )).containsExactly("ADMIN", "AGENT", "CUSTOMER", "MANAGER", "OWNER");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class))
                .isEqualTo(6);
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM permissions ORDER BY code",
                String.class
        )).containsExactly(
                "PERMISSION_MANAGE",
                "PERMISSION_READ",
                "ROLE_MANAGE",
                "ROLE_READ",
                "USER_MANAGE",
                "USER_READ"
        );
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_permissions
                JOIN roles ON roles.id = role_permissions.role_id
                WHERE roles.code = 'ADMIN'
                """,
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_permissions
                JOIN roles ON roles.id = role_permissions.role_id
                WHERE roles.code = 'MANAGER'
                """,
                Integer.class
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM property_types ORDER BY display_order",
                String.class
        )).containsExactly(
                "APARTMENT",
                "HOUSE",
                "VILLA",
                "LAND",
                "OFFICE",
                "RETAIL",
                "WAREHOUSE"
        );
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM amenities ORDER BY display_order",
                String.class
        )).containsExactly(
                "PARKING",
                "ELEVATOR",
                "SECURITY",
                "SWIMMING_POOL",
                "GYM",
                "BALCONY",
                "AIR_CONDITIONING",
                "FURNISHED"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM provinces", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'PROPERTIES'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        )).isEqualTo(5);
    }
}
