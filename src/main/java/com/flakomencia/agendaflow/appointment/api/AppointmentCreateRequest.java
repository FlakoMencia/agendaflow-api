package com.flakomencia.agendaflow.appointment.api;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AppointmentCreateRequest(
        @NotNull @Positive Long customerId,
        @NotNull @Positive Long branchId,
        @NotNull @Positive Long serviceId,
        @NotNull @Positive Long specialistId,
        @NotNull OffsetDateTime startsAt,
        String customerNotes,
        String internalNotes) {
}
