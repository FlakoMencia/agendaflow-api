package com.flakomencia.agendaflow.scheduling.api;

import java.time.OffsetDateTime;

public record AvailableSlot(Long specialistId, OffsetDateTime start, OffsetDateTime end) {
}
