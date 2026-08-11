package com.flakomencia.agendaflow.appointment.api;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.appointment.domain.AppointmentOrigin;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;

public record AppointmentResponse(
        Long id, Long organizationId, Long branchId, Long customerId, Long serviceId, Long specialistId,
        OffsetDateTime startsAt, OffsetDateTime endsAt, AppointmentStatus status, AppointmentOrigin origin,
        String customerNotes, String internalNotes, String cancellationReason, OffsetDateTime cancelledAt,
        Long cancelledBy, Long createdBy, Long updatedBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
