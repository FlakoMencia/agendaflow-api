package com.flakomencia.agendaflow.identity.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email, password or organization is invalid");
    }
}
