package com.flakomencia.agendaflow.specialist.api;

import java.time.OffsetDateTime;

public record SpecialistBranchResponse(
        Long specialistId,
        Long branchId,
        String branchName,
        boolean primary,
        boolean active,
        OffsetDateTime createdAt) {
}
