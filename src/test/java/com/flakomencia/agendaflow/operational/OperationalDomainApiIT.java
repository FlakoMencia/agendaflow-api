package com.flakomencia.agendaflow.operational;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class OperationalDomainApiIT {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:18.4")
            .withDatabaseName("agendaflow_operational_test")
            .withUsername("agendaflow_user");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    private String platformToken;

    @BeforeEach
    void cleanAndAuthenticate() {
        jdbc.update("DELETE FROM agendaflow.schedule_blocks");
        jdbc.update("DELETE FROM agendaflow.availability_schedules");
        jdbc.update("DELETE FROM agendaflow.specialist_services");
        jdbc.update("DELETE FROM agendaflow.specialist_branches");
        jdbc.update("DELETE FROM agendaflow.specialists");
        jdbc.update("DELETE FROM agendaflow.branch_services");
        jdbc.update("DELETE FROM agendaflow.services");
        jdbc.update("DELETE FROM agendaflow.service_categories");
        jdbc.update("DELETE FROM agendaflow.membership_roles");
        jdbc.update("DELETE FROM agendaflow.organization_memberships");
        jdbc.update("DELETE FROM agendaflow.app_users");
        jdbc.update("DELETE FROM agendaflow.branches");
        jdbc.update("DELETE FROM agendaflow.organizations");
        platformToken = createSession(createOrganization("Platform"), "PLATFORM_ADMIN");
    }

    @Test
    void managesCatalogAndRejectsCrossTenantBranchAssignments() throws Exception {
        Long organizationId = createOrganization("Wellness One");
        Long otherOrganizationId = createOrganization("Wellness Two");
        Long branchId = createBranch(organizationId, "Central");
        Long otherBranchId = createBranch(otherOrganizationId, "Other");

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/service-categories", organizationId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Therapy","description":"Clinical services"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true));
        Long categoryId = jdbc.queryForObject(
                "SELECT id FROM agendaflow.service_categories WHERE organization_id=?", Long.class, organizationId);

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/services", organizationId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"name":"Initial consultation","durationMinutes":45,
                                 "preparationMinutes":5,"cleanupMinutes":10,"price":75.50,"currencyCode":"USD"}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.price").value(75.50));
        Long serviceId = jdbc.queryForObject(
                "SELECT id FROM agendaflow.services WHERE organization_id=?", Long.class, organizationId);

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/services", organizationId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/services/{serviceId}", organizationId, serviceId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"name":"Extended consultation","durationMinutes":60,
                                 "preparationMinutes":5,"cleanupMinutes":10,"price":90.00,"currencyCode":"USD",
                                 "requiresApproval":true,"allowsOnlineBooking":false,"active":true}
                                """.formatted(categoryId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Extended consultation"));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/branches/{branchId}/services/{serviceId}",
                        organizationId, branchId, serviceId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serviceId").value(serviceId));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/branches/{branchId}/services/{serviceId}",
                        otherOrganizationId, otherBranchId, serviceId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void managesSpecialistsAndBothAssignmentsWithTenantIsolation() throws Exception {
        Long organizationId = createOrganization("Specialist Org");
        Long otherOrganizationId = createOrganization("Foreign Org");
        Long branchId = createBranch(organizationId, "Main");
        Long foreignBranchId = createBranch(otherOrganizationId, "Foreign");
        Long serviceId = createService(organizationId, "Massage");
        Long foreignServiceId = createService(otherOrganizationId, "Foreign service");

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists", organizationId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalName":"Alex Rivera","specialtyName":"Therapist",
                                 "simultaneousCapacity":1,"active":true}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.professionalName").value("Alex Rivera"));
        Long specialistId = jdbc.queryForObject(
                "SELECT id FROM agendaflow.specialists WHERE organization_id=?", Long.class, organizationId);

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/specialists/{specialistId}/branches/{branchId}",
                        organizationId, specialistId, branchId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primary\":true,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.primary").value(true));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/specialists/{specialistId}/services/{serviceId}",
                        organizationId, specialistId, serviceId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customDurationMinutes\":50,\"customPrice\":80.00,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customDurationMinutes").value(50));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/specialists/{specialistId}/branches/{branchId}",
                        organizationId, specialistId, foreignBranchId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/organizations/{organizationId}/specialists/{specialistId}/services/{serviceId}",
                        organizationId, specialistId, foreignServiceId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void validatesAvailabilityRangesBranchesAndOverlaps() throws Exception {
        Long organizationId = createOrganization("Schedule Org");
        Long otherOrganizationId = createOrganization("Other Schedule Org");
        Long branchId = createBranch(organizationId, "Schedule Branch");
        Long foreignBranchId = createBranch(otherOrganizationId, "Foreign Branch");
        Long specialistId = createSpecialist(organizationId, "Schedule Specialist");

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/availability",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"dayOfWeek":1,"startTime":"09:00:00","endTime":"12:00:00",
                                 "validFrom":"2026-01-01","active":true}
                                """.formatted(branchId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dayOfWeek").value(1));

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/availability",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"dayOfWeek":2,"startTime":"12:00:00","endTime":"12:00:00"}
                                """.formatted(branchId)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/availability",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"dayOfWeek":1,"startTime":"11:00:00","endTime":"13:00:00",
                                 "validFrom":"2026-02-01","active":true}
                                """.formatted(branchId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SCHEDULE_OVERLAP"));

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/availability",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"dayOfWeek":3,"startTime":"09:00:00","endTime":"10:00:00"}
                                """.formatted(foreignBranchId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));
    }

    @Test
    void managesScheduleBlocksAndRejectsForeignBranch() throws Exception {
        Long organizationId = createOrganization("Blocks Org");
        Long otherOrganizationId = createOrganization("Other Blocks Org");
        Long branchId = createBranch(organizationId, "Blocks Branch");
        Long foreignBranchId = createBranch(otherOrganizationId, "Foreign Blocks Branch");
        Long specialistId = createSpecialist(organizationId, "Blocked Specialist");
        Long foreignSpecialistId = createSpecialist(otherOrganizationId, "Foreign Specialist");

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"blockType":"VACATION","startsAt":"2026-08-10T08:00:00-06:00",
                                 "endsAt":"2026-08-12T17:00:00-06:00","reason":"Annual leave"}
                                """.formatted(branchId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.blockType").value("VACATION"));
        Long blockId = jdbc.queryForObject(
                "SELECT id FROM agendaflow.schedule_blocks WHERE organization_id=?", Long.class, organizationId);

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks",
                        organizationId, specialistId).header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks/{blockId}",
                        organizationId, specialistId, blockId).header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(blockId));

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"blockType":"MEETING","startsAt":"2026-08-10T10:00:00-06:00",
                                 "endsAt":"2026-08-10T09:00:00-06:00"}
                                """.formatted(branchId)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks",
                        organizationId, specialistId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":%d,"blockType":"MEETING","startsAt":"2026-08-10T09:00:00-06:00",
                                 "endsAt":"2026-08-10T10:00:00-06:00"}
                                """.formatted(foreignBranchId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));

        Long foreignBlockId = jdbc.queryForObject("""
                INSERT INTO agendaflow.schedule_blocks
                    (organization_id,specialist_id,block_type,starts_at,ends_at)
                VALUES (?,?,'PERSONAL','2026-09-01T09:00:00-06:00','2026-09-01T10:00:00-06:00')
                RETURNING id
                """, Long.class, otherOrganizationId, foreignSpecialistId);
        mockMvc.perform(get(
                        "/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks/{blockId}",
                        organizationId, specialistId, foreignBlockId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_BLOCK_NOT_FOUND"));
    }

    @Test
    void enforcesAuthenticationPermissionsAndRouteTenant() throws Exception {
        Long organizationId = createOrganization("Security Org");
        Long otherOrganizationId = createOrganization("Other Security Org");
        String receptionistToken = createSession(organizationId, "RECEPTIONIST");

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/services", organizationId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/organizations/{organizationId}/services", organizationId)
                        .header("Authorization", bearer(receptionistToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Denied\",\"durationMinutes\":30}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/organizations/{organizationId}/services", otherOrganizationId)
                        .header("Authorization", bearer(receptionistToken)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void exposesOnlyDocumentedNonDeleteOperationalRoutes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == 'Service Catalog')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Specialists')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Availability')]").exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations/{organizationId}/services'].delete").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/organizations/{organizationId}/specialists/{specialistId}/schedule-blocks'].post").exists());
    }

    private Long createOrganization(String name) {
        return jdbc.queryForObject("INSERT INTO agendaflow.organizations (legal_name) VALUES (?) RETURNING id",
                Long.class, name);
    }

    private Long createBranch(Long organizationId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO agendaflow.branches (organization_id,name) VALUES (?,?) RETURNING id",
                Long.class, organizationId, name);
    }

    private Long createService(Long organizationId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO agendaflow.services (organization_id,name,duration_minutes) VALUES (?,?,30) RETURNING id",
                Long.class, organizationId, name);
    }

    private Long createSpecialist(Long organizationId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO agendaflow.specialists (organization_id,professional_name) VALUES (?,?) RETURNING id",
                Long.class, organizationId, name);
    }

    private String createSession(Long organizationId, String role) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        String email = "operational-%d@test.local".formatted(sequence);
        String password = "operational-test-password";
        Long userId = jdbc.queryForObject("""
                INSERT INTO agendaflow.app_users (email,password_hash,first_name,last_name)
                VALUES (?,?,'Operational','Tester') RETURNING id
                """, Long.class, email, passwordEncoder.encode(password));
        Long membershipId = jdbc.queryForObject("""
                INSERT INTO agendaflow.organization_memberships (organization_id,user_id)
                VALUES (?,?) RETURNING id
                """, Long.class, organizationId, userId);
        jdbc.update("""
                INSERT INTO agendaflow.membership_roles (membership_id,role_id)
                SELECT ?,id FROM agendaflow.roles WHERE organization_id IS NULL AND name=?
                """, membershipId, role);
        try {
            String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"%s","organizationId":%d}
                                    """.formatted(email, password, organizationId)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return objectMapper.readTree(response).get("accessToken").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create integration-test session", exception);
        }
    }

    private String bearer(String token) { return "Bearer " + token; }
}
