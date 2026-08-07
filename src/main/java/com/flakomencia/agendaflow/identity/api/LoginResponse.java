package com.flakomencia.agendaflow.identity.api;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long membershipId,
        AuthenticatedUserResponse user,
        ActiveOrganizationResponse activeOrganization,
        List<String> roles,
        List<String> permissions) {

    public LoginResponse {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
