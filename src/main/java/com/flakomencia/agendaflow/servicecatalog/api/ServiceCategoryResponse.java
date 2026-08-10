package com.flakomencia.agendaflow.servicecatalog.api;

import java.time.OffsetDateTime;

public record ServiceCategoryResponse(
        Long id,
        Long organizationId,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
