package com.flakomencia.agendaflow.scheduling.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flakomencia.agendaflow.common.api.StandardApiErrorResponses;
import com.flakomencia.agendaflow.scheduling.application.AvailableSlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/availability/slots",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Availability", description = "Effective appointment availability")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class AvailableSlotController {
    private final AvailableSlotService slots;
    public AvailableSlotController(AvailableSlotService slots) { this.slots = slots; }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW') or hasAuthority('APPOINTMENTS_CREATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Find effective slots", description = "Uses branch timezone, weekly schedules, blocks, service buffers, specialist capacity and existing appointments.")
    public AvailableSlotsResponse find(@Positive @PathVariable Long organizationId,
            @Positive @RequestParam Long branchId, @Positive @RequestParam Long serviceId,
            @Parameter(example = "2026-08-15") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate date,
            @Positive @RequestParam(required = false) Long specialistId) {
        return slots.findSlots(organizationId, branchId, serviceId, date, specialistId);
    }
}
