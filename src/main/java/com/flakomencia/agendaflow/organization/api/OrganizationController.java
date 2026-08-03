package com.flakomencia.agendaflow.organization.api;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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
import com.flakomencia.agendaflow.organization.application.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(path = "/api/v1/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Organizations", description = "Organization management")
@StandardApiErrorResponses
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an organization")
    @ApiResponse(responseCode = "201", description = "Organization created")
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{organizationId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List active organization records")
    @ApiResponse(responseCode = "200", description = "Paginated organizations")
    public PageResponse<OrganizationResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "legalName") Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get an organization")
    @ApiResponse(responseCode = "200", description = "Organization found")
    public OrganizationResponse get(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId) {
        return service.get(organizationId);
    }

    @PutMapping(path = "/{organizationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an organization")
    @ApiResponse(responseCode = "200", description = "Organization updated")
    public OrganizationResponse update(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        return service.update(organizationId, request);
    }
}
