package com.flakomencia.agendaflow.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AgendaFlowJwtAuthenticationConverterTest {
    @Test
    void mapsPermissionsAndRolePrefixedAuthoritiesWithoutScopeDefaults() {
        Jwt jwt = new Jwt(
                "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "1", "permissions", List.of("ORGANIZATION_VIEW"), "roles", List.of("MANAGER")));

        var authentication = new AgendaFlowJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ORGANIZATION_VIEW", "ROLE_MANAGER");
    }
}
