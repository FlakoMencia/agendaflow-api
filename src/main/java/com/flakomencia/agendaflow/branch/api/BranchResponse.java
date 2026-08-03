package com.flakomencia.agendaflow.branch.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BranchResponse(
        Long id,
        Long organizationId,
        String name,
        String code,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String stateCode,
        String postalCode,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
