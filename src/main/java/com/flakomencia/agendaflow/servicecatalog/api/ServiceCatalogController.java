package com.flakomencia.agendaflow.servicecatalog.api;

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
import com.flakomencia.agendaflow.servicecatalog.application.ServiceCatalogApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Service Catalog", description = "Tenant-isolated service categories and bookable services")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class ServiceCatalogController {

    private final ServiceCatalogApplicationService service;

    public ServiceCatalogController(ServiceCatalogApplicationService service) { this.service = service; }

    @PostMapping(path = "/service-categories", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SERVICES_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a service category", description = "Requires SERVICES_MANAGE; organization must match the authenticated tenant")
    @ApiResponse(responseCode = "201", description = "Service category created")
    public ResponseEntity<ServiceCategoryResponse> createCategory(
            @Positive @PathVariable Long organizationId,
            @Valid @RequestBody ServiceCategoryCreateRequest request) {
        ServiceCategoryResponse response = service.createCategory(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{categoryId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/service-categories")
    @PreAuthorize("hasAuthority('SERVICES_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List service categories", description = "Requires SERVICES_VIEW; returns only the requested organization")
    public List<ServiceCategoryResponse> listCategories(@Positive @PathVariable Long organizationId) {
        return service.listCategories(organizationId);
    }

    @GetMapping("/service-categories/{categoryId}")
    @PreAuthorize("hasAuthority('SERVICES_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get a service category", description = "Requires SERVICES_VIEW")
    public ServiceCategoryResponse getCategory(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long categoryId) {
        return service.getCategory(organizationId, categoryId);
    }

    @PutMapping(path = "/service-categories/{categoryId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SERVICES_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update a service category", description = "Requires SERVICES_MANAGE")
    public ServiceCategoryResponse updateCategory(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long categoryId,
            @Valid @RequestBody ServiceCategoryUpdateRequest request) {
        return service.updateCategory(organizationId, categoryId, request);
    }

    @PostMapping(path = "/services", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SERVICES_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a bookable service", description = "Requires SERVICES_MANAGE")
    @ApiResponse(responseCode = "201", description = "Service created")
    public ResponseEntity<CatalogServiceResponse> createService(
            @Positive @PathVariable Long organizationId, @Valid @RequestBody CatalogServiceCreateRequest request) {
        CatalogServiceResponse response = service.createService(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{serviceId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/services")
    @PreAuthorize("hasAuthority('SERVICES_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List services", description = "Requires SERVICES_VIEW; returns a stable paginated response")
    public PageResponse<CatalogServiceResponse> listServices(
            @Positive @PathVariable Long organizationId,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.listServices(organizationId, pageable);
    }

    @GetMapping("/services/{serviceId}")
    @PreAuthorize("hasAuthority('SERVICES_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get a service", description = "Requires SERVICES_VIEW")
    public CatalogServiceResponse getService(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long serviceId) {
        return service.getService(organizationId, serviceId);
    }

    @PutMapping(path = "/services/{serviceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SERVICES_MANAGE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update a service", description = "Requires SERVICES_MANAGE")
    public CatalogServiceResponse updateService(
            @Positive @PathVariable Long organizationId, @Positive @PathVariable Long serviceId,
            @Valid @RequestBody CatalogServiceUpdateRequest request) {
        return service.updateService(organizationId, serviceId, request);
    }
}
