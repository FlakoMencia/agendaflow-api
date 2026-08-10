package com.flakomencia.agendaflow.scheduling.api;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AvailabilityUpdateRequest(
        @NotNull @Positive Long branchId,
        @NotNull @Min(0) @Max(6) Short dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean active) {
}
