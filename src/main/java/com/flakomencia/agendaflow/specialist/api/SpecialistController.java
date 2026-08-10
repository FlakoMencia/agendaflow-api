package com.flakomencia.agendaflow.specialist.api;

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
import com.flakomencia.agendaflow.specialist.application.SpecialistApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/specialists",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Specialists", description = "Tenant-isolated specialists and their branch/service assignments")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class SpecialistController {

    private final SpecialistApplicationService service;

    public SpecialistController(SpecialistApplicationService service) { this.service = service; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SPECIALISTS_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a specialist", description = "Requires SPECIALISTS_MANAGE")
    @ApiResponse(responseCode = "201", description = "Specialist created")
    public ResponseEntity<SpecialistResponse> create(
            @Positive @PathVariable Long organizationId, @Valid @RequestBody SpecialistCreateRequest request) {
        SpecialistResponse response = service.create(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{specialistId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SPECIALISTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List specialists", description = "Requires SPECIALISTS_VIEW; returns a stable paginated response")
    public PageResponse<SpecialistResponse> list(
            @Positive @PathVariable Long organizationId,
            @ParameterObject @PageableDefault(size = 20, sort = "professionalName") Pageable pageable) {
        return service.list(organizationId, pageable);
    }

    @GetMapping("/{specialistId}")
    @PreAuthorize("hasAuthority('SPECIALISTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get a specialist", description = "Requires SPECIALISTS_VIEW")
    public SpecialistResponse get(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId) {
        return service.get(organizationId, specialistId);
    }

    @PutMapping(path = "/{specialistId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SPECIALISTS_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update a specialist", description = "Requires SPECIALISTS_MANAGE")
    public SpecialistResponse update(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Valid @RequestBody SpecialistUpdateRequest request) {
        return service.update(organizationId, specialistId, request);
    }

    @GetMapping("/{specialistId}/branches")
    @PreAuthorize("hasAuthority('SPECIALISTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List a specialist's branch assignments", description = "Requires SPECIALISTS_VIEW")
    public List<SpecialistBranchResponse> listBranches(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId) {
        return service.listBranches(organizationId, specialistId);
    }

    @PutMapping(path = "/{specialistId}/branches/{branchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SPECIALISTS_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create or update a specialist-branch assignment", description = "Requires SPECIALISTS_MANAGE; both resources must belong to the organization")
    public SpecialistBranchResponse assignBranch(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Positive @PathVariable Long branchId, @Valid @RequestBody SpecialistBranchAssignmentRequest request) {
        return service.assignBranch(organizationId, specialistId, branchId, request);
    }

    @GetMapping("/{specialistId}/services")
    @PreAuthorize("hasAuthority('SPECIALISTS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List a specialist's service assignments", description = "Requires SPECIALISTS_VIEW")
    public List<SpecialistServiceResponse> listServices(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId) {
        return service.listServices(organizationId, specialistId);
    }

    @PutMapping(path = "/{specialistId}/services/{serviceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SPECIALISTS_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create or update a specialist-service assignment", description = "Requires SPECIALISTS_MANAGE; both resources must belong to the organization")
    public SpecialistServiceResponse assignService(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long specialistId,
            @Positive @PathVariable Long serviceId, @Valid @RequestBody SpecialistServiceAssignmentRequest request) {
        return service.assignService(organizationId, specialistId, serviceId, request);
    }
}
