package com.flakomencia.agendaflow.scheduling.api;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.api.StandardApiErrorResponses;
import com.flakomencia.agendaflow.scheduling.application.SchedulingApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/specialists/{specialistId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Availability", description = "Recurring availability rules and schedule blocks; does not generate booking slots")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class SchedulingController {

    private final SchedulingApplicationService service;

    public SchedulingController(SchedulingApplicationService service) { this.service = service; }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List recurring availability", description = "Requires SCHEDULE_VIEW")
    public List<AvailabilityResponse> listAvailability(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId) {
        return service.listAvailability(organizationId, specialistId);
    }

    @PostMapping(path = "/availability", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create recurring availability", description = "Requires SCHEDULE_MANAGE; rejects exact duplicates and overlapping active rules")
    @ApiResponse(responseCode = "201", description = "Availability created")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Valid @RequestBody AvailabilityCreateRequest request) {
        AvailabilityResponse response = service.createAvailability(organizationId, specialistId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{scheduleId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(path = "/availability/{scheduleId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update recurring availability", description = "Requires SCHEDULE_MANAGE")
    public AvailabilityResponse updateAvailability(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Positive @PathVariable Long scheduleId, @Valid @RequestBody AvailabilityUpdateRequest request) {
        return service.updateAvailability(organizationId, specialistId, scheduleId, request);
    }

    @PostMapping(path = "/schedule-blocks", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a schedule block", description = "Requires SCHEDULE_MANAGE")
    @ApiResponse(responseCode = "201", description = "Schedule block created")
    public ResponseEntity<ScheduleBlockResponse> createBlock(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Valid @RequestBody ScheduleBlockCreateRequest request) {
        ScheduleBlockResponse response = service.createBlock(organizationId, specialistId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{blockId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/schedule-blocks")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List schedule blocks", description = "Requires SCHEDULE_VIEW; returns a stable paginated response")
    public PageResponse<ScheduleBlockResponse> listBlocks(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @ParameterObject @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return service.listBlocks(organizationId, specialistId, pageable);
    }

    @GetMapping("/schedule-blocks/{blockId}")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get a schedule block", description = "Requires SCHEDULE_VIEW")
    public ScheduleBlockResponse getBlock(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Positive @PathVariable Long blockId) {
        return service.getBlock(organizationId, specialistId, blockId);
    }

    @PutMapping(path = "/schedule-blocks/{blockId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update a schedule block", description = "Requires SCHEDULE_MANAGE")
    public ScheduleBlockResponse updateBlock(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Positive @PathVariable Long blockId, @Valid @RequestBody ScheduleBlockUpdateRequest request) {
        return service.updateBlock(organizationId, specialistId, blockId, request);
    }
}
