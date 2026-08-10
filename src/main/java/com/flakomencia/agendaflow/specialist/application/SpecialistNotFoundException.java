package com.flakomencia.agendaflow.specialist.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class SpecialistNotFoundException extends ApiException {
    public SpecialistNotFoundException(Long organizationId, Long specialistId) {
        super(HttpStatus.NOT_FOUND, "SPECIALIST_NOT_FOUND",
                "Specialist %d was not found in organization %d".formatted(specialistId, organizationId));
    }
}
