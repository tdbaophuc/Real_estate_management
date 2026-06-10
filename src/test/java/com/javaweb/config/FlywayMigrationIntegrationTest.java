package com.javaweb.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanListingSchemaTestData() {
        jdbcTemplate.update(
                """
                DELETE FROM leads
                WHERE code LIKE 'LEAD-MIGRATION-%'
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM users
                WHERE email IN (
                    'lead-migration-agent@example.com',
                    'lead-migration-creator@example.com'
                )
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM customer_favorite_listings
                WHERE customer_id IN (
                    SELECT id FROM customers WHERE code LIKE 'CUSTOMER-MIGRATION-%'
                )
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM customer_notes
                WHERE customer_id IN (
                    SELECT id FROM customers WHERE code LIKE 'CUSTOMER-MIGRATION-%'
                )
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM customer_tags
                WHERE customer_id IN (
                    SELECT id FROM customers WHERE code LIKE 'CUSTOMER-MIGRATION-%'
                )
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM customer_requirements
                WHERE customer_id IN (
                    SELECT id FROM customers WHERE code LIKE 'CUSTOMER-MIGRATION-%'
                )
                """
        );
        jdbcTemplate.update(
                "DELETE FROM customers WHERE code LIKE 'CUSTOMER-MIGRATION-%'"
        );
        jdbcTemplate.update(
                """
                DELETE FROM users
                WHERE email IN (
                    'customer-creator@example.com',
                    'customer-account@example.com'
                )
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM listing_favorites
                WHERE listing_id IN (SELECT id FROM listings WHERE code = 'LISTING-001')
                """
        );
        jdbcTemplate.update(
                """
                DELETE FROM listing_status_histories
                WHERE listing_id IN (SELECT id FROM listings WHERE code = 'LISTING-001')
                """
        );
        jdbcTemplate.update("DELETE FROM listings WHERE code = 'LISTING-001'");
        jdbcTemplate.update("DELETE FROM properties WHERE code = 'PROPERTY-LISTING-001'");
        jdbcTemplate.update(
                """
                DELETE FROM addresses
                WHERE full_address = '16 Listing Street, Listing Province'
                """
        );
        jdbcTemplate.update("DELETE FROM provinces WHERE code = 'LISTING-PROVINCE'");
        jdbcTemplate.update(
                """
                DELETE FROM users
                WHERE email IN (
                    'listing-owner@example.com',
                    'listing-customer@example.com'
                )
                """
        );
    }

    @Test
    void shouldApplyDatabaseMigrationsAndSeedMasterData() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getDescription())
                .isEqualTo("create lead pipeline schema");

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
                "file_resources",
                "listing_packages",
                "listings",
                "listing_status_histories",
                "listing_views",
                "listing_favorites",
                "customers",
                "customer_requirements",
                "customer_tags",
                "customer_notes",
                "customer_favorite_listings",
                "lead_sources",
                "leads",
                "lead_assignments",
                "lead_notes",
                "lead_activities",
                "follow_up_tasks",
                "flyway_schema_history"
        );
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM lead_sources ORDER BY code",
                String.class
        )).containsExactly(
                "CHATBOT",
                "IMPORT",
                "LISTING_INQUIRY",
                "MANUAL",
                "OTHER",
                "REFERRAL",
                "WEBSITE"
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
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'PROPERTY_IMAGES'
                  AND column_name = 'FILE_RESOURCE_ID'
                """,
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'LISTINGS'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        )).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'LISTINGS'
                  AND constraint_type = 'CHECK'
                """,
                Integer.class
        )).isGreaterThanOrEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'LISTING_FAVORITES'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'LISTINGS'
                  AND column_name IN (
                      'PROPERTY_ID',
                      'STATUS',
                      'SUBMITTED_AT',
                      'REVIEWED_AT',
                      'PUBLISHED_AT',
                      'EXPIRES_AT'
                  )
                """,
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'CUSTOMERS'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC'
                  AND table_name = 'CUSTOMER_FAVORITE_LISTINGS'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void shouldEnforceLeadPipelineConstraints() {
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified)
                VALUES ('lead-migration-agent@example.com', 'hash', 'Lead Agent', 'ACTIVE', TRUE),
                       ('lead-migration-creator@example.com', 'hash', 'Lead Creator', 'ACTIVE', TRUE)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO leads (
                    code,
                    source_id,
                    current_assignee_id,
                    created_by,
                    full_name,
                    phone,
                    status,
                    score
                )
                SELECT
                    'LEAD-MIGRATION-VALID',
                    source.id,
                    agent.id,
                    creator.id,
                    'Migration Prospect',
                    '0900000023',
                    'ASSIGNED',
                    80
                FROM lead_sources source
                CROSS JOIN users agent
                CROSS JOIN users creator
                WHERE source.code = 'WEBSITE'
                  AND agent.email = 'lead-migration-agent@example.com'
                  AND creator.email = 'lead-migration-creator@example.com'
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM leads
                WHERE code = 'LEAD-MIGRATION-VALID'
                """,
                String.class
        )).isEqualTo("ASSIGNED");

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO leads (code, source_id, full_name, phone, status)
                SELECT 'LEAD-MIGRATION-STATUS', id, 'Invalid Status', '0900000024', 'QUALIFIED'
                FROM lead_sources
                WHERE code = 'MANUAL'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO leads (code, source_id, full_name, phone, score)
                SELECT 'LEAD-MIGRATION-SCORE', id, 'Invalid Score', '0900000025', 101
                FROM lead_sources
                WHERE code = 'MANUAL'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO follow_up_tasks (
                    lead_id,
                    assigned_to,
                    created_by,
                    title,
                    status,
                    due_at
                )
                SELECT
                    lead.id,
                    agent.id,
                    creator.id,
                    'Invalid completed task',
                    'COMPLETED',
                    CURRENT_TIMESTAMP
                FROM leads lead
                CROSS JOIN users agent
                CROSS JOIN users creator
                WHERE lead.code = 'LEAD-MIGRATION-VALID'
                  AND agent.email = 'lead-migration-agent@example.com'
                  AND creator.email = 'lead-migration-creator@example.com'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldSupportAccountLinkedAndOfflineCustomersWithCrmConstraints() {
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified)
                VALUES ('customer-creator@example.com', 'hash', 'Customer Creator', 'ACTIVE', TRUE),
                       ('customer-account@example.com', 'hash', 'Customer Account', 'ACTIVE', TRUE)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO customers (
                    code,
                    user_id,
                    created_by,
                    full_name,
                    email,
                    source,
                    priority
                )
                SELECT
                    'CUSTOMER-MIGRATION-LINKED',
                    account.id,
                    creator.id,
                    'Linked Customer',
                    account.email,
                    'WEBSITE',
                    'HIGH'
                FROM users account
                CROSS JOIN users creator
                WHERE account.email = 'customer-account@example.com'
                  AND creator.email = 'customer-creator@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO customers (code, created_by, full_name, phone)
                SELECT
                    'CUSTOMER-MIGRATION-OFFLINE',
                    id,
                    'Offline Customer',
                    '0900000021'
                FROM users
                WHERE email = 'customer-creator@example.com'
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM customers
                WHERE code LIKE 'CUSTOMER-MIGRATION-%'
                """,
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT user_id IS NULL
                FROM customers
                WHERE code = 'CUSTOMER-MIGRATION-OFFLINE'
                """,
                Boolean.class
        )).isTrue();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO customers (code, user_id, created_by, full_name, email)
                SELECT
                    'CUSTOMER-MIGRATION-DUPLICATE',
                    account.id,
                    creator.id,
                    'Duplicate Account Customer',
                    account.email
                FROM users account
                CROSS JOIN users creator
                WHERE account.email = 'customer-account@example.com'
                  AND creator.email = 'customer-creator@example.com'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO customer_requirements (
                    customer_id,
                    purpose,
                    min_budget,
                    max_budget
                )
                SELECT id, 'SALE', 5000000000, 1000000000
                FROM customers
                WHERE code = 'CUSTOMER-MIGRATION-LINKED'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "DELETE FROM users WHERE email = 'customer-account@example.com'"
        );
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT user_id IS NULL
                FROM customers
                WHERE code = 'CUSTOMER-MIGRATION-LINKED'
                """,
                Boolean.class
        )).isTrue();
    }

    @Test
    void shouldKeepListingWorkflowSeparateFromPropertyAndEnforceListingConstraints() {
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified)
                VALUES ('listing-owner@example.com', 'hash', 'Listing Owner', 'ACTIVE', TRUE),
                       ('listing-customer@example.com', 'hash', 'Listing Customer', 'ACTIVE', TRUE)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO provinces (code, name)
                VALUES ('LISTING-PROVINCE', 'Listing Province')
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO addresses (province_id, street_address, full_address)
                SELECT id, '16 Listing Street', '16 Listing Street, Listing Province'
                FROM provinces
                WHERE code = 'LISTING-PROVINCE'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO properties (
                    code,
                    property_type_id,
                    address_id,
                    created_by,
                    name,
                    purpose,
                    status,
                    price
                )
                SELECT
                    'PROPERTY-LISTING-001',
                    property_types.id,
                    addresses.id,
                    users.id,
                    'Property for listing schema test',
                    'SALE',
                    'DRAFT',
                    2500000000
                FROM property_types
                CROSS JOIN addresses
                CROSS JOIN users
                WHERE property_types.code = 'APARTMENT'
                  AND addresses.full_address = '16 Listing Street, Listing Province'
                  AND users.email = 'listing-owner@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO listings (
                    code,
                    property_id,
                    created_by,
                    title,
                    slug,
                    description,
                    purpose,
                    asking_price
                )
                SELECT
                    'LISTING-001',
                    properties.id,
                    users.id,
                    'Apartment for sale',
                    'apartment-for-sale-listing-001',
                    'Listing description',
                    'SALE',
                    2600000000
                FROM properties
                CROSS JOIN users
                WHERE properties.code = 'PROPERTY-LISTING-001'
                  AND users.email = 'listing-owner@example.com'
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM listings WHERE code = 'LISTING-001'",
                String.class
        )).isEqualTo("DRAFT");

        jdbcTemplate.update(
                """
                INSERT INTO listing_status_histories (
                    listing_id,
                    from_status,
                    to_status,
                    changed_by,
                    reason
                )
                SELECT listings.id, 'DRAFT', 'PENDING_REVIEW', users.id, 'Submitted for review'
                FROM listings
                CROSS JOIN users
                WHERE listings.code = 'LISTING-001'
                  AND users.email = 'listing-owner@example.com'
                """
        );
        jdbcTemplate.update(
                """
                UPDATE listings
                SET status = 'PENDING_REVIEW', submitted_at = CURRENT_TIMESTAMP
                WHERE code = 'LISTING-001'
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM properties WHERE code = 'PROPERTY-LISTING-001'",
                String.class
        )).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM listing_status_histories WHERE to_status = 'PENDING_REVIEW'",
                Integer.class
        )).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE listings SET status = 'INVALID' WHERE code = 'LISTING-001'"
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                """
                INSERT INTO listing_favorites (listing_id, user_id)
                SELECT listings.id, users.id
                FROM listings
                CROSS JOIN users
                WHERE listings.code = 'LISTING-001'
                  AND users.email = 'listing-customer@example.com'
                """
        );
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO listing_favorites (listing_id, user_id)
                SELECT listings.id, users.id
                FROM listings
                CROSS JOIN users
                WHERE listings.code = 'LISTING-001'
                  AND users.email = 'listing-customer@example.com'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
