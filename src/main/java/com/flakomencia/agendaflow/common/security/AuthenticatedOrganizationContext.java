package com.flakomencia.agendaflow.common.security;

import java.util.Set;

public record AuthenticatedOrganizationContext(
        Long userId,
        Long membershipId,
        Long organizationId,
        Set<String> roles,
        Set<String> permissions) {

    public AuthenticatedOrganizationContext {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    public boolean isPlatformAdministrator() {
        return roles.contains("PLATFORM_ADMIN");
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
