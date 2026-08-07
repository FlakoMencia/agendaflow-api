package com.flakomencia.agendaflow.identity.api;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String preferredLanguage) {
}
