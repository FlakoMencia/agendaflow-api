package com.flakomencia.agendaflow.appointment.application;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.common.exception.ConflictException;

public class AppointmentNotDueException extends ConflictException {
    public AppointmentNotDueException(OffsetDateTime startsAt) {
        super("APPOINTMENT_NOT_DUE", "Appointment cannot be marked as no-show before " + startsAt);
    }
}
