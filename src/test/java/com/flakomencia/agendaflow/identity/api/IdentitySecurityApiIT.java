package com.flakomencia.agendaflow.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.flakomencia.agendaflow.common.security.AgendaFlowSecurityProperties;
import com.flakomencia.agendaflow.identity.application.ServiceAccessTokenService;

import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class IdentitySecurityApiIT {
    private static final String POSTGRES_IMAGE = "postgres:18.4";

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("agendaflow_identity_test")
            .withUsername("agendaflow_user");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired AgendaFlowSecurityProperties properties;
    @Autowired @Qualifier("userJwtDecoder") JwtDecoder userJwtDecoder;
    @Autowired @Qualifier("userJwtEncoder") JwtEncoder userJwtEncoder;
    @Autowired @Qualifier("serviceJwtDecoder") JwtDecoder serviceJwtDecoder;
    @Autowired ServiceAccessTokenService serviceAccessTokenService;

    @BeforeEach
    void cleanIdentityData() {
        jdbcTemplate.update("DELETE FROM agendaflow.membership_branches");
        jdbcTemplate.update("DELETE FROM agendaflow.membership_roles");
        jdbcTemplate.update("DELETE FROM agendaflow.organization_memberships");
        jdbcTemplate.update("DELETE FROM agendaflow.app_users");
        jdbcTemplate.update("DELETE FROM agendaflow.branches");
        jdbcTemplate.update("DELETE FROM agendaflow.organizations");
    }

    @Test
    void loginIssuesTenantTokenAndMeRevalidatesTheSession() throws Exception {
        Long organizationId = organization("Tenant One", "ACTIVE");
        Long userId = user("manager@example.com", "correct-password", true, false);
        Long membershipId = membership(organizationId, userId, "ACTIVE", "MANAGER");

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("manager@example.com", "correct-password", organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.activeOrganization.id").value(organizationId))
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("accessToken").asText();
        var jwt = userJwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(((Number) jwt.getClaim("membership_id")).longValue()).isEqualTo(membershipId);
        assertThat(((Number) jwt.getClaim("organization_id")).longValue()).isEqualTo(organizationId);
        assertThat(jwt.getClaimAsString("token_use")).isEqualTo("user");
        assertThat(jwt.getClaimAsStringList("permissions")).contains("ORGANIZATION_VIEW", "BRANCHES_VIEW");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("manager@example.com"))
                .andExpect(jsonPath("$.activeOrganization.id").value(organizationId))
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"));
    }

    @Test
    void usesGenericInvalidCredentialsForAllLoginIdentityFailures() throws Exception {
        Long activeOrganization = organization("Active", "ACTIVE");
        Long suspendedOrganization = organization("Suspended", "SUSPENDED");
        Long userId = user("user@example.com", "correct-password", true, false);

        assertInvalidLogin("missing@example.com", "wrong", activeOrganization);
        assertInvalidLogin("user@example.com", "wrong", activeOrganization);
        assertInvalidLogin("user@example.com", "correct-password", activeOrganization);

        membership(activeOrganization, userId, "SUSPENDED", "MANAGER");
        assertInvalidLogin("user@example.com", "correct-password", activeOrganization);

        Long secondUser = user("second@example.com", "correct-password", true, false);
        membership(suspendedOrganization, secondUser, "ACTIVE", "MANAGER");
        assertInvalidLogin("second@example.com", "correct-password", suspendedOrganization);
    }

    @Test
    void locksUserAtConfiguredFailedAttemptThreshold() throws Exception {
        Long organizationId = organization("Lockout", "ACTIVE");
        Long userId = user("lockout@example.com", "correct-password", true, false);
        membership(organizationId, userId, "ACTIVE", "MANAGER");

        for (int attempt = 0; attempt < properties.lockout().maximumFailedAttempts(); attempt++) {
            assertInvalidLogin("lockout@example.com", "wrong-password", organizationId);
        }

        var state = jdbcTemplate.queryForMap(
                "SELECT failed_login_attempts, is_locked FROM agendaflow.app_users WHERE id = ?", userId);
        assertThat(((Number) state.get("failed_login_attempts")).intValue())
                .isEqualTo(properties.lockout().maximumFailedAttempts());
        assertThat(state.get("is_locked")).isEqualTo(true);
        assertInvalidLogin("lockout@example.com", "correct-password", organizationId);
    }

    @Test
    void returnsUniformErrorsForMissingInvalidAndExpiredBearerTokens() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(expiredToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void enforcesPermissionsAndHidesCrossTenantOrganizationsAndBranches() throws Exception {
        Long ownOrganization = organization("Own tenant", "ACTIVE");
        Long otherOrganization = organization("Other tenant", "ACTIVE");
        Long userId = user("auditor@example.com", "correct-password", true, false);
        membership(ownOrganization, userId, "ACTIVE", "AUDITOR");
        String token = login("auditor@example.com", "correct-password", ownOrganization);

        mockMvc.perform(get("/api/v1/organizations/{id}", ownOrganization)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/organizations/{id}", otherOrganization)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
        mockMvc.perform(put("/api/v1/organizations/{id}", ownOrganization)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalName\":\"Forbidden update\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalName\":\"Forbidden create\"}"))
                .andExpect(status().isForbidden());

        Long branchId = jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.branches (organization_id, name)
                VALUES (?, 'Hidden branch') RETURNING id
                """, Long.class, otherOrganization);
        mockMvc.perform(get("/api/v1/organizations/{organizationId}/branches/{branchId}", otherOrganization, branchId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void platformAdministratorCanCreateAndListAllOrganizations() throws Exception {
        Long platformOrganization = organization("Platform home", "ACTIVE");
        Long otherOrganization = organization("Existing tenant", "ACTIVE");
        Long userId = user("platform@example.com", "correct-password", true, false);
        membership(platformOrganization, userId, "ACTIVE", "PLATFORM_ADMIN");
        String token = login("platform@example.com", "correct-password", platformOrganization);

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalName\":\"Created tenant\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", bearer(token))
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        assertThat(otherOrganization).isPositive();
    }

    @Test
    void openApiDocumentsLoginMeAndBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void internalServiceTokenHasExpectedAudienceAndValidationGroup() {
        var jwt = serviceJwtDecoder.decode(serviceAccessTokenService.issueNotificationValidationToken().value());

        assertThat(jwt.getSubject()).isEqualTo("agendaflow-api");
        assertThat(jwt.getAudience()).containsExactly("agendaflow-notification-service");
        assertThat(jwt.getClaimAsString("token_use")).isEqualTo("service");
        assertThat(jwt.getClaimAsStringList("groups")).containsExactly("notification:validate");
    }

    private void assertInvalidLogin(String email, String password, Long organizationId) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password, organizationId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email, password or organization is invalid"));
    }

    private String login(String email, String password, Long organizationId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password, organizationId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long organization(String legalName, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.organizations (legal_name, status)
                VALUES (?, ?) RETURNING id
                """, Long.class, legalName, status);
    }

    private Long user(String email, String password, boolean active, boolean locked) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.app_users
                    (email, password_hash, first_name, last_name, is_active, is_locked)
                VALUES (?, ?, 'Test', 'User', ?, ?) RETURNING id
                """, Long.class, email, passwordEncoder.encode(password), active, locked);
    }

    private Long membership(Long organizationId, Long userId, String status, String roleName) {
        Long membershipId = jdbcTemplate.queryForObject("""
                INSERT INTO agendaflow.organization_memberships (organization_id, user_id, status)
                VALUES (?, ?, ?) RETURNING id
                """, Long.class, organizationId, userId, status);
        jdbcTemplate.update("""
                INSERT INTO agendaflow.membership_roles (membership_id, role_id)
                SELECT ?, id FROM agendaflow.roles
                WHERE organization_id IS NULL AND name = ?
                """, membershipId, roleName);
        return membershipId;
    }

    private String expiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.userToken().issuer())
                .audience(List.of(properties.userToken().audience()))
                .subject("1")
                .issuedAt(now.minusSeconds(600))
                .notBefore(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(120))
                .id("expired-test-token")
                .claim("user_id", 1L)
                .claim("membership_id", 1L)
                .claim("organization_id", 1L)
                .claim("roles", List.of("MANAGER"))
                .claim("permissions", List.of("ORGANIZATION_VIEW"))
                .claim("token_use", "user")
                .build();
        return userJwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }

    private String loginJson(String email, String password, Long organizationId) {
        return """
                {"email":"%s","password":"%s","organizationId":%d}
                """.formatted(email, password, organizationId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
