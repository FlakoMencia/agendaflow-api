package com.flakomencia.agendaflow.servicecatalog.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CatalogServiceResponse(
        Long id,
        Long organizationId,
        Long categoryId,
        String name,
        String description,
        int durationMinutes,
        int preparationMinutes,
        int cleanupMinutes,
        BigDecimal price,
        String currencyCode,
        boolean requiresApproval,
        boolean allowsOnlineBooking,
        boolean active,
        String colorCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
