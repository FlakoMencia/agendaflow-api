package com.flakomencia.agendaflow.appointment.application;

import com.flakomencia.agendaflow.common.exception.ConflictException;

public class AppointmentAlreadyCancelledException extends ConflictException {
    public AppointmentAlreadyCancelledException() {
        super("APPOINTMENT_ALREADY_CANCELLED", "The appointment is already cancelled");
    }
}
