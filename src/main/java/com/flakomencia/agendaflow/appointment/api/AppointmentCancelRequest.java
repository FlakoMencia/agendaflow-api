package com.flakomencia.agendaflow.appointment.api;

import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(@Size(max = 10000) String reason) {
}
