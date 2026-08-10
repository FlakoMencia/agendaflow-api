package com.flakomencia.agendaflow.scheduling.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class ScheduleBlockNotFoundException extends ApiException {
    public ScheduleBlockNotFoundException(Long organizationId, Long specialistId, Long blockId) {
        super(HttpStatus.NOT_FOUND, "SCHEDULE_BLOCK_NOT_FOUND",
                "Schedule block %d was not found for specialist %d in organization %d"
                        .formatted(blockId, specialistId, organizationId));
    }
}
