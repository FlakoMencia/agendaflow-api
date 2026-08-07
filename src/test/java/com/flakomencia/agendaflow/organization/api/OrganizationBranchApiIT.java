package com.flakomencia.agendaflow.organization.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class OrganizationBranchApiIT {

    private static final String POSTGRES_IMAGE = "postgres:18.4";

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("agendaflow_vertical_slice_test")
            .withUsername("agendaflow_user");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String platformToken;

    @BeforeEach
    void cleanVerticalSliceData() {
        jdbcTemplate.update("DELETE FROM agendaflow.membership_roles");
        jdbcTemplate.update("DELETE FROM agendaflow.organization_memberships");
        jdbcTemplate.update("DELETE FROM agendaflow.app_users");
        jdbcTemplate.update("DELETE FROM agendaflow.branches");
        jdbcTemplate.update("DELETE FROM agendaflow.organizations");
        platformToken = createPlatformSession();
    }

    @Test
    void createsGetsUpdatesAndPagesOrganizations() throws Exception {
        Long firstId = createOrganization("Northwind Health", "TAX-NORTH");
        createOrganization("AgendaFlow Wellness", "TAX-AGENDA");

        mockMvc.perform(get("/api/v1/organizations/{organizationId}", firstId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.legalName").value("Northwind Health"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

                mockMvc.perform(put("/api/v1/organizations/{organizationId}", firstId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Northwind Health Group",
                                  "taxIdentifier": "TAX-NORTH",
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Northwind Health Group"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", bearer())
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .queryParam("sort", "legalName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].legalName").value("AgendaFlow Wellness"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void keepsBranchAccessScopedToOrganization() throws Exception {
        Long ownerOrganizationId = createOrganization("Owner", "TAX-OWNER");
        Long otherOrganizationId = createOrganization("Other", "TAX-OTHER");

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/branches", ownerOrganizationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Central Branch",
                                  "code": "CENTRAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/branches/[0-9]+$")))
                .andExpect(jsonPath("$.organizationId").value(ownerOrganizationId))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.active").value(true));

        Long branchId = jdbcTemplate.queryForObject(
                "SELECT id FROM agendaflow.branches WHERE organization_id = ? AND code = ?",
                Long.class,
                ownerOrganizationId,
                "CENTRAL");

        mockMvc.perform(get("/api/v1/organizations/{organizationId}/branches", ownerOrganizationId)
                        .header("Authorization", bearer())
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(branchId));

        mockMvc.perform(get(
                        "/api/v1/organizations/{organizationId}/branches/{branchId}",
                        ownerOrganizationId,
                        branchId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Central Branch"));

        mockMvc.perform(put(
                        "/api/v1/organizations/{organizationId}/branches/{branchId}",
                        ownerOrganizationId,
                        branchId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Central Office",
                                  "code": "CENTRAL",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Central Office"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get(
                        "/api/v1/organizations/{organizationId}/branches/{branchId}",
                        otherOrganizationId,
                        branchId).header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/organizations/%d/branches/%d".formatted(otherOrganizationId, branchId)));
    }

    @Test
    void returnsUniformValidationAndDuplicateErrors() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName": "   ", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.legalName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        createOrganization("First", "TAX-DUPLICATE");

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName": "Second", "taxIdentifier": "TAX-DUPLICATE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_TAX_IDENTIFIER_EXISTS"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void allowsConfiguredDevelopmentCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/organizations")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization")));
    }

    @Test
    void documentsOnlySupportedOrganizationAndBranchOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/organizations'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations'].get").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/organizations'].post.responses['400'].content['application/json'].schema")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations'].delete").doesNotExist())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/organizations/{organizationId}/branches/{branchId}'].put").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Organizations')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Branches')]").exists());
    }

    private Long createOrganization(String legalName, String taxIdentifier) throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "%s",
                                  "taxIdentifier": "%s"
                                }
                                """.formatted(legalName, taxIdentifier)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.legalName").value(legalName));

        return jdbcTemplate.queryForObject(
                "SELECT id FROM agendaflow.organizations WHERE tax_identifier = ?",
                Long.class,
                taxIdentifier);
    }

    private String bearer() {
        return "Bearer " + platformToken;
    }

    private String createPlatformSession() {
        Long organizationId = jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.organizations (legal_name, status)
                VALUES ('Platform administration', 'ACTIVE')
                RETURNING id
                """, Long.class);
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.app_users (email, password_hash, first_name, last_name)
                VALUES (?, ?, 'Platform', 'Administrator')
                RETURNING id
                """, Long.class, "platform@test.local", passwordEncoder.encode("platform-test-password"));
        Long membershipId = jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.organization_memberships (organization_id, user_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, organizationId, userId);
        jdbcTemplate.update("""
                INSERT INTO agendaflow.membership_roles (membership_id, role_id)
                SELECT ?, id FROM agendaflow.roles
                WHERE organization_id IS NULL AND name = 'PLATFORM_ADMIN'
                """, membershipId);
        try {
            String response = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"platform@test.local","password":"platform-test-password","organizationId":%d}
                                    """.formatted(organizationId)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return objectMapper.readTree(response).get("accessToken").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create integration-test platform session", exception);
        }
    }
}
