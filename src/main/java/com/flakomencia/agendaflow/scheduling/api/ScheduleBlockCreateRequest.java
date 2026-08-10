package com.flakomencia.agendaflow.scheduling.api;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlockType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScheduleBlockCreateRequest(
        @Positive Long branchId,
        @NotNull ScheduleBlockType blockType,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        String reason,
        Boolean active) {
}
