package com.flakomencia.agendaflow.specialist.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpecialistServiceResponse(
        Long specialistId,
        Long serviceId,
        String serviceName,
        Integer customDurationMinutes,
        BigDecimal customPrice,
        boolean active,
        OffsetDateTime createdAt) {
}
