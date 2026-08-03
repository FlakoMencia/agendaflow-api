package com.flakomencia.agendaflow.branch.api;

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

import com.flakomencia.agendaflow.branch.application.BranchService;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.api.StandardApiErrorResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping(
        path = "/api/v1/organizations/{organizationId}/branches",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Branches", description = "Organization-scoped branch management")
@StandardApiErrorResponses
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a branch inside an organization")
    @ApiResponse(responseCode = "201", description = "Branch created")
    public ResponseEntity<BranchResponse> create(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId,
            @Valid @RequestBody BranchCreateRequest request) {
        BranchResponse response = service.create(organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{branchId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List branches in an organization")
    @ApiResponse(responseCode = "200", description = "Paginated branches")
    public PageResponse<BranchResponse> list(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(organizationId, pageable);
    }

    @GetMapping("/{branchId}")
    @Operation(summary = "Get an organization-scoped branch")
    @ApiResponse(responseCode = "200", description = "Branch found")
    public BranchResponse get(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId,
            @Parameter(description = "Branch identifier", example = "10")
            @Positive @PathVariable Long branchId) {
        return service.get(organizationId, branchId);
    }

    @PutMapping(path = "/{branchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an organization-scoped branch")
    @ApiResponse(responseCode = "200", description = "Branch updated")
    public BranchResponse update(
            @Parameter(description = "Organization identifier", example = "1")
            @Positive @PathVariable Long organizationId,
            @Parameter(description = "Branch identifier", example = "10")
            @Positive @PathVariable Long branchId,
            @Valid @RequestBody BranchUpdateRequest request) {
        return service.update(organizationId, branchId, request);
    }
}
