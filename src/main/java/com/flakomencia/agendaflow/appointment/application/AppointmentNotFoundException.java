package com.flakomencia.agendaflow.appointment.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class AppointmentNotFoundException extends ApiException {
    public AppointmentNotFoundException(Long organizationId, Long appointmentId) {
        super(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND",
                "Appointment %d was not found in organization %d".formatted(appointmentId, organizationId));
    }
}
