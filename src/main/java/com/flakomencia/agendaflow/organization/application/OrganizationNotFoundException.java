package com.flakomencia.agendaflow.organization.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class OrganizationNotFoundException extends ApiException {

    public OrganizationNotFoundException(Long organizationId) {
        super(HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND",
                "Organization %d was not found".formatted(organizationId));
    }
}
