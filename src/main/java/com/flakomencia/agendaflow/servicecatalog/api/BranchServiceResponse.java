package com.flakomencia.agendaflow.servicecatalog.api;

import java.time.OffsetDateTime;

public record BranchServiceResponse(
        Long branchId,
        Long serviceId,
        String serviceName,
        boolean active,
        OffsetDateTime createdAt) {
}
