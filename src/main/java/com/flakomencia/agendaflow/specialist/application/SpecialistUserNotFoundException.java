package com.flakomencia.agendaflow.specialist.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class SpecialistUserNotFoundException extends ApiException {
    public SpecialistUserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SPECIALIST_USER_NOT_FOUND",
                "User was not found as an active member of the organization");
    }
}
