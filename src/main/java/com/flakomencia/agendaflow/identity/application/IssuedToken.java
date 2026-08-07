package com.flakomencia.agendaflow.identity.application;

public record IssuedToken(String value, long expiresInSeconds) { }
