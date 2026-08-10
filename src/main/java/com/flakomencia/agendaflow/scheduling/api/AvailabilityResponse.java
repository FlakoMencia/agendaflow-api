package com.flakomencia.agendaflow.scheduling.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AvailabilityResponse(
        Long id,
        Long organizationId,
        Long specialistId,
        Long branchId,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
