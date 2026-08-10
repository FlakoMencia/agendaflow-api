package com.flakomencia.agendaflow.specialist.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

public record SpecialistServiceAssignmentRequest(
        @Positive Integer customDurationMinutes,
        @Digits(integer = 10, fraction = 2) @DecimalMin("0.00") BigDecimal customPrice,
        Boolean active) {
}
