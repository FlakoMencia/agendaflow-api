package com.flakomencia.agendaflow.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
class FlywayMigrationIT {

    private static final String POSTGRES_IMAGE = "postgres:18.4";

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "agendaflow.organizations",
            "agendaflow.branches",
            "agendaflow.app_users",
            "agendaflow.organization_memberships",
            "agendaflow.roles",
            "agendaflow.permissions",
            "agendaflow.role_permissions",
            "agendaflow.membership_roles",
            "agendaflow.membership_branches",
            "agendaflow.organization_contacts",
            "agendaflow.organization_owners",
            "agendaflow.service_categories",
            "agendaflow.services",
            "agendaflow.branch_services",
            "agendaflow.specialists",
            "agendaflow.specialist_branches",
            "agendaflow.specialist_services",
            "agendaflow.availability_schedules",
            "agendaflow.schedule_blocks",
            "agendaflow.resources",
            "agendaflow.resource_services",
            "agendaflow.customers",
            "agendaflow.appointments",
            "agendaflow.appointment_status_history",
            "agendaflow.waitlist_requests",
            "agendaflow.notification_templates",
            "agendaflow.notifications",
            "agendaflow.notification_outbox",
            "audit.audit_events");

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("agendaflow_integration_test")
            .withUsername("agendaflow_user");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    void createsExpectedSchemasAndTables() {
        List<String> schemas = jdbcTemplate.queryForList(
                "SELECT schema_name FROM information_schema.schemata",
                String.class);

        assertThat(schemas).contains("agendaflow", "audit");

        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_schema || '.' || table_name
                FROM information_schema.tables
                WHERE table_schema IN ('agendaflow', 'audit')
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);

        assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
        assertThat(historyTableExists()).isTrue();
    }

    @Test
    void recordsOnlySuccessfulVersionedMigrationsWithoutBaseline() {
        Long failedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agendaflow.flyway_schema_history WHERE success = FALSE",
                Long.class);
        Long baselineEntries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agendaflow.flyway_schema_history WHERE type = 'BASELINE'",
                Long.class);
        Long successfulVersionedMigrations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM agendaflow.flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                """, Long.class);

        assertThat(failedMigrations).isZero();
        assertThat(baselineEntries).isZero();
        assertThat(successfulVersionedMigrations).isEqualTo(3L);
        assertThat(environment.getProperty("spring.flyway.baseline-on-migrate"))
                .isEqualTo("false");
    }

    @Test
    void assignsEveryActivePermissionToGlobalPlatformAdministrator() {
        Long activePermissions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agendaflow.permissions WHERE is_active = TRUE", Long.class);
        Long platformPermissions = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM agendaflow.role_permissions role_permission
                JOIN agendaflow.roles role ON role.id = role_permission.role_id
                WHERE role.organization_id IS NULL
                  AND role.name = 'PLATFORM_ADMIN'
                """, Long.class);

        assertThat(activePermissions).isPositive();
        assertThat(platformPermissions).isEqualTo(activePermissions);
    }

    @Test
    void usesBigintForIdsAndForeignKeys() {
        Long idColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema IN ('agendaflow', 'audit')
                  AND column_name = 'id'
                """, Long.class);
        List<Map<String, Object>> nonBigintIds = jdbcTemplate.queryForList("""
                SELECT table_schema, table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema IN ('agendaflow', 'audit')
                  AND column_name = 'id'
                  AND data_type <> 'bigint'
                """);
        Long foreignKeyColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND kcu.table_schema IN ('agendaflow', 'audit')
                """, Long.class);
        List<Map<String, Object>> nonBigintForeignKeys = jdbcTemplate.queryForList("""
                SELECT kcu.table_schema, kcu.table_name, kcu.column_name, c.data_type
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                JOIN information_schema.columns c
                  ON c.table_schema = kcu.table_schema
                 AND c.table_name = kcu.table_name
                 AND c.column_name = kcu.column_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND kcu.table_schema IN ('agendaflow', 'audit')
                  AND c.data_type <> 'bigint'
                """);

        assertThat(idColumnCount).isPositive();
        assertThat(nonBigintIds).isEmpty();
        assertThat(foreignKeyColumnCount).isPositive();
        assertThat(nonBigintForeignKeys).isEmpty();
    }

    @Test
    void keepsHibernateAndBatchInValidationOnlyMode() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(environment.getProperty("spring.batch.jdbc.initialize-schema"))
                .isEqualTo("never");
    }

    private boolean historyTableExists() {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'agendaflow'
                      AND table_name = 'flyway_schema_history'
                )
                """, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
