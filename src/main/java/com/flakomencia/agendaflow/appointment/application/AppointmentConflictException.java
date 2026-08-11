package com.flakomencia.agendaflow.appointment.application;

import com.flakomencia.agendaflow.common.exception.ConflictException;

public class AppointmentConflictException extends ConflictException {
    public AppointmentConflictException() {
        super("APPOINTMENT_CONFLICT", "The requested appointment conflicts with current availability");
    }
}
