package com.flakomencia.agendaflow.identity.api;

import java.util.List;

public record CurrentSessionResponse(
        Long membershipId,
        AuthenticatedUserResponse user,
        ActiveOrganizationResponse activeOrganization,
        List<String> roles,
        List<String> permissions) {

    public CurrentSessionResponse {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
