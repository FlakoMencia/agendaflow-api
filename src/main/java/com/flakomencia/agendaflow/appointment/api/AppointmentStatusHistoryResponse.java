package com.flakomencia.agendaflow.appointment.api;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;

public record AppointmentStatusHistoryResponse(
        Long id, AppointmentStatus previousStatus, AppointmentStatus newStatus,
        Long changedBy, String changeReason, OffsetDateTime changedAt) {
}
