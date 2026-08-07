package com.flakomencia.agendaflow.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;

class TenantAccessGuardTest {
    @Test
    void hidesCrossTenantResourcesFromRegularUsers() {
        var resolver = mock(AuthenticatedOrganizationContextResolver.class);
        when(resolver.current()).thenReturn(new AuthenticatedOrganizationContext(
                1L, 2L, 10L, Set.of("MANAGER"), Set.of("ORGANIZATION_VIEW")));

        assertThatThrownBy(() -> new TenantAccessGuard(resolver).requireTenant(99L))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void allowsPlatformAdministratorTenantBypass() {
        var resolver = mock(AuthenticatedOrganizationContextResolver.class);
        when(resolver.current()).thenReturn(new AuthenticatedOrganizationContext(
                1L, 2L, 10L, Set.of("PLATFORM_ADMIN"), Set.of()));

        new TenantAccessGuard(resolver).requireTenant(99L);
    }
}
