package com.flakomencia.agendaflow.identity.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class InvalidSessionException extends ApiException {
    public InvalidSessionException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The authenticated session is no longer valid");
    }
}
