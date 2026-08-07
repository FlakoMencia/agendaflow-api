package com.flakomencia.agendaflow.common.security;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;

@Component
public class TenantAccessGuard {
    private final AuthenticatedOrganizationContextResolver contexts;

    public TenantAccessGuard(AuthenticatedOrganizationContextResolver contexts) {
        this.contexts = contexts;
    }

    public AuthenticatedOrganizationContext current() {
        return contexts.current();
    }

    public void requireTenant(Long requestedOrganizationId) {
        AuthenticatedOrganizationContext context = current();
        if (!context.isPlatformAdministrator() && !context.organizationId().equals(requestedOrganizationId)) {
            throw new OrganizationNotFoundException(requestedOrganizationId);
        }
    }
}
