package com.flakomencia.agendaflow.scheduling.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class AvailabilityNotFoundException extends ApiException {
    public AvailabilityNotFoundException(Long organizationId, Long specialistId, Long scheduleId) {
        super(HttpStatus.NOT_FOUND, "AVAILABILITY_NOT_FOUND",
                "Availability %d was not found for specialist %d in organization %d"
                        .formatted(scheduleId, specialistId, organizationId));
    }
}
