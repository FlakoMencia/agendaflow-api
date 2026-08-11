package com.flakomencia.agendaflow.customer.api;

import java.net.URI;

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
import com.flakomencia.agendaflow.customer.application.CustomerApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations/{organizationId}/customers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Customers", description = "Organization-scoped customer records")
@StandardApiErrorResponses
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    private final CustomerApplicationService service;
    public CustomerController(CustomerApplicationService service) { this.service = service; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMERS_CREATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create customer", description = "Requires CUSTOMERS_CREATE; email is optional.")
    public ResponseEntity<CustomerResponse> create(@Positive @PathVariable Long organizationId,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMERS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List customers", description = "Requires CUSTOMERS_VIEW; all results are tenant-scoped.")
    public PageResponse<CustomerResponse> list(@Positive @PathVariable Long organizationId,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return service.list(organizationId, pageable);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('CUSTOMERS_VIEW') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get customer")
    public CustomerResponse get(@Positive @PathVariable Long organizationId, @Positive @PathVariable Long customerId) {
        return service.get(organizationId, customerId);
    }

    @PutMapping(path = "/{customerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMERS_UPDATE') or hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update customer", description = "Requires CUSTOMERS_UPDATE; no automatic customer merging is performed.")
    public CustomerResponse update(@Positive @PathVariable Long organizationId, @Positive @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest request) {
        return service.update(organizationId, customerId, request);
    }
}
