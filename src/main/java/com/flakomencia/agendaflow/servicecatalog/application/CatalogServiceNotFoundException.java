package com.flakomencia.agendaflow.servicecatalog.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class CatalogServiceNotFoundException extends ApiException {
    public CatalogServiceNotFoundException(Long organizationId, Long serviceId) {
        super(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND",
                "Service %d was not found in organization %d".formatted(serviceId, organizationId));
    }
}
