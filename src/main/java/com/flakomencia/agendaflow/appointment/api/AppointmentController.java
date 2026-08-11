package com.flakomencia.agendaflow.appointment.api;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flakomencia.agendaflow.appointment.application.AppointmentApplicationService;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.api.StandardApiErrorResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Appointments", description = "Transactional organization-scoped booking lifecycle")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {
    private final AppointmentApplicationService service;
    public AppointmentController(AppointmentApplicationService service) { this.service = service; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('APPOINTMENTS_CREATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create appointment", description = "Locks the specialist and recalculates effective capacity before insert; requires APPOINTMENTS_CREATE.")
    public ResponseEntity<AppointmentResponse> create(@Positive @PathVariable Long organizationId,
            @Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentResponse response = service.create(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List appointments", description = "Tenant-scoped pagination with bounded date, branch, specialist, customer and status filters.")
    public PageResponse<AppointmentResponse> list(@Positive @PathVariable Long organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @Positive @RequestParam(required = false) Long branchId,
            @Positive @RequestParam(required = false) Long specialistId,
            @Positive @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) AppointmentStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return service.list(organizationId, from, to, branchId, specialistId, customerId, status, pageable);
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAuthority('APPOINTMENTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get appointment")
    public AppointmentResponse get(@Positive @PathVariable Long organizationId, @Positive @PathVariable Long appointmentId) {
        return service.get(organizationId, appointmentId);
    }

    @PostMapping(path = "/{appointmentId}/reschedule", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('APPOINTMENTS_UPDATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Reschedule appointment", description = "Reuses the booking availability and locking algorithm; the appointment itself is excluded from conflicts.")
    public AppointmentResponse reschedule(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId, @Valid @RequestBody AppointmentRescheduleRequest request) {
        return service.reschedule(organizationId, appointmentId, request);
    }

    @PostMapping(path = "/{appointmentId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('APPOINTMENTS_CANCEL') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Cancel appointment", description = "Soft lifecycle operation; a repeated cancellation returns a 409 conflict.")
    public AppointmentResponse cancel(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId, @Valid @RequestBody AppointmentCancelRequest request) {
        return service.cancel(organizationId, appointmentId, request);
    }

    @PostMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasAuthority('APPOINTMENTS_UPDATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Confirm appointment")
    public AppointmentResponse confirm(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.confirm(organizationId, appointmentId);
    }

    @PostMapping("/{appointmentId}/check-in")
    @PreAuthorize("hasAuthority('APPOINTMENTS_UPDATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Check in appointment")
    public AppointmentResponse checkIn(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.checkIn(organizationId, appointmentId);
    }

    @PostMapping("/{appointmentId}/start")
    @PreAuthorize("hasAuthority('APPOINTMENTS_UPDATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Start appointment service")
    public AppointmentResponse start(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.start(organizationId, appointmentId);
    }

    @PostMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAuthority('APPOINTMENTS_COMPLETE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Complete appointment")
    public AppointmentResponse complete(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.complete(organizationId, appointmentId);
    }

    @PostMapping("/{appointmentId}/no-show")
    @PreAuthorize("hasAuthority('APPOINTMENTS_COMPLETE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Mark appointment as no-show", description = "Allowed only after the scheduled instant.")
    public AppointmentResponse markNoShow(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.markNoShow(organizationId, appointmentId);
    }

    @GetMapping("/{appointmentId}/history")
    @PreAuthorize("hasAuthority('APPOINTMENTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get immutable appointment status history")
    public List<AppointmentStatusHistoryResponse> history(@Positive @PathVariable Long organizationId,
            @Positive @PathVariable Long appointmentId) {
        return service.history(organizationId, appointmentId);
    }
}
