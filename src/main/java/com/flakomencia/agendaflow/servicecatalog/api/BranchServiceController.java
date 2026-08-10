package com.flakomencia.agendaflow.servicecatalog.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flakomencia.agendaflow.common.api.StandardApiErrorResponses;
import com.flakomencia.agendaflow.servicecatalog.application.ServiceCatalogApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/branches/{branchId}/services",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Service Catalog", description = "Tenant-isolated service categories and bookable services")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class BranchServiceController {

    private final ServiceCatalogApplicationService service;

    public BranchServiceController(ServiceCatalogApplicationService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('SERVICES_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List services assigned to a branch", description = "Requires SERVICES_VIEW")
    public List<BranchServiceResponse> list(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long branchId) {
        return service.listBranchServices(organizationId, branchId);
    }

    @PutMapping(path = "/{serviceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SERVICES_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create or update a branch-service assignment", description = "Requires SERVICES_MANAGE; both resources must belong to the organization")
    public BranchServiceResponse assign(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long branchId,
            @Positive @PathVariable Long serviceId, @Valid @RequestBody BranchServiceAssignmentRequest request) {
        return service.assignBranchService(organizationId, branchId, serviceId, request);
    }
}
