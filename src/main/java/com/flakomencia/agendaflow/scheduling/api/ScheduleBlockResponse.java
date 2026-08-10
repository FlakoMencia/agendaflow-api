package com.flakomencia.agendaflow.scheduling.api;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlockType;

public record ScheduleBlockResponse(
        Long id,
        Long organizationId,
        Long specialistId,
        Long branchId,
        ScheduleBlockType blockType,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String reason,
        boolean active,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
