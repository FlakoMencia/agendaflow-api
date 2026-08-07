package com.flakomencia.agendaflow.identity.api;

import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;

public record ActiveOrganizationResponse(
        Long id,
        String legalName,
        String tradeName,
        String timezone,
        String languageCode,
        OrganizationStatus status) {
}
