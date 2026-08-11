package com.flakomencia.agendaflow.notification.application;

import java.time.OffsetDateTime;
import java.util.Map;

public record AppointmentNotificationPayload(
        Long eventId,
        Long organizationId,
        Long appointmentId,
        String type,
        String recipient,
        String locale,
        OffsetDateTime occurredAt,
        Map<String, String> variables) {
}
