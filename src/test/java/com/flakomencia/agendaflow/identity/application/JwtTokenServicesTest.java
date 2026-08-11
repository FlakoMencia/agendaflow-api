package com.flakomencia.agendaflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.flakomencia.agendaflow.common.security.AgendaFlowSecurityProperties;

class JwtTokenServicesTest {
    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");

    @Test
    void issuesUserTokenWithTenantRolesPermissionsAndRegisteredClaims() {
        String secret = "user-test-secret-with-more-than-thirty-two-bytes";
        var settings = new AgendaFlowSecurityProperties.Token(secret, "agendaflow-api", "agendaflow-web", Duration.ofMinutes(30));
        var service = new UserAccessTokenService(
                NimbusJwtEncoder.withSecretKey(key(secret)).algorithm(MacAlgorithm.HS256).build(),
                properties(settings), identifiers(), Clock.fixed(NOW, ZoneOffset.UTC));

        IssuedToken issued = service.issue(new AuthenticatedIdentity(
                11L, 22L, 33L, List.of("MANAGER"), List.of("BRANCHES_VIEW")));
        var jwt = decoder(secret).decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo("11");
        assertThat(jwt.getClaimAsString("token_use")).isEqualTo("user");
        assertThat(((Number) jwt.getClaim("membership_id")).longValue()).isEqualTo(22L);
        assertThat(((Number) jwt.getClaim("organization_id")).longValue()).isEqualTo(33L);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("MANAGER");
        assertThat(jwt.getClaimAsStringList("permissions")).containsExactly("BRANCHES_VIEW");
        assertThat(jwt.getAudience()).containsExactly("agendaflow-web");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getNotBefore()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(issued.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    void issuesIndependentInternalServiceToken() {
        String secret = "service-test-secret-with-more-than-thirty-two-bytes";
        var serviceSettings = new AgendaFlowSecurityProperties.Token(
                secret, "agendaflow-api", "agendaflow-notification-service", Duration.ofMinutes(5));
        var properties = new AgendaFlowSecurityProperties(
                new AgendaFlowSecurityProperties.Lockout(5),
                new AgendaFlowSecurityProperties.Token("u".repeat(32), "agendaflow-api", "web", Duration.ofMinutes(30)),
                serviceSettings);
        var service = new ServiceAccessTokenService(
                NimbusJwtEncoder.withSecretKey(key(secret)).algorithm(MacAlgorithm.HS256).build(),
                properties, identifiers(), Clock.fixed(NOW, ZoneOffset.UTC));

        var jwt = decoder(secret).decode(service.issueNotificationValidationToken().value());

        assertThat(jwt.getSubject()).isEqualTo("agendaflow-api");
        assertThat(jwt.getClaimAsString("token_use")).isEqualTo("service");
        assertThat(jwt.getClaimAsStringList("groups")).containsExactly("notification:validate");
        assertThat(jwt.getAudience()).containsExactly("agendaflow-notification-service");

        var submissionJwt = decoder(secret).decode(service.issueNotificationSubmissionToken().value());
        assertThat(submissionJwt.getClaimAsStringList("groups")).containsExactly("notification:submit");
        assertThat(submissionJwt.getSubject()).isEqualTo("agendaflow-api");
        assertThat(submissionJwt.getClaimAsString("token_use")).isEqualTo("service");
    }

    private AgendaFlowSecurityProperties properties(AgendaFlowSecurityProperties.Token userSettings) {
        return new AgendaFlowSecurityProperties(
                new AgendaFlowSecurityProperties.Lockout(5), userSettings,
                new AgendaFlowSecurityProperties.Token("s".repeat(32), "issuer", "service", Duration.ofMinutes(5)));
    }

    private JwtIdentifierGenerator identifiers() {
        return new JwtIdentifierGenerator(new SecureRandom());
    }

    private JwtDecoder decoder(String secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
