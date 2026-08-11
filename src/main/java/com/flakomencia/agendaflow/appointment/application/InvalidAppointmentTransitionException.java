package com.flakomencia.agendaflow.appointment.application;

import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;
import com.flakomencia.agendaflow.common.exception.ConflictException;

public class InvalidAppointmentTransitionException extends ConflictException {
    public InvalidAppointmentTransitionException(AppointmentStatus status, String action) {
        super("INVALID_APPOINTMENT_TRANSITION",
                "Appointment in status %s cannot be %s".formatted(status, action));
    }
}
