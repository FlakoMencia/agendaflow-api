package com.flakomencia.agendaflow.identity.application;

import java.util.List;

public record AuthenticatedIdentity(
        Long userId,
        Long membershipId,
        Long organizationId,
        List<String> roles,
        List<String> permissions) {

    public AuthenticatedIdentity {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
