package com.flakomencia.agendaflow.scheduling.api;

import java.time.LocalDate;
import java.util.List;

public record AvailableSlotsResponse(
        LocalDate date, Long branchId, Long serviceId, String timezone, List<AvailableSlot> slots) {
    public AvailableSlotsResponse { slots = List.copyOf(slots); }
}
