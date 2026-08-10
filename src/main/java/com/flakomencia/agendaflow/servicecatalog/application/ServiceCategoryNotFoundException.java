package com.flakomencia.agendaflow.servicecatalog.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class ServiceCategoryNotFoundException extends ApiException {
    public ServiceCategoryNotFoundException(Long organizationId, Long categoryId) {
        super(HttpStatus.NOT_FOUND, "SERVICE_CATEGORY_NOT_FOUND",
                "Service category %d was not found in organization %d".formatted(categoryId, organizationId));
    }
}
