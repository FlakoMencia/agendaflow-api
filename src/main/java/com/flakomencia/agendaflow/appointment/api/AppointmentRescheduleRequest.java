package com.flakomencia.agendaflow.appointment.api;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AppointmentRescheduleRequest(
        @NotNull OffsetDateTime startsAt,
        @Positive Long specialistId,
        String reason) {
}
