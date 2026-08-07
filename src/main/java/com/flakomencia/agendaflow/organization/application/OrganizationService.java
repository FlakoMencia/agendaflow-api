package com.flakomencia.agendaflow.organization.application;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.api.OrganizationCreateRequest;
import com.flakomencia.agendaflow.organization.api.OrganizationResponse;
import com.flakomencia.agendaflow.organization.api.OrganizationUpdateRequest;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

@Service
public class OrganizationService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "legalName", "tradeName", "status", "createdAt", "updatedAt");

    private final OrganizationRepository repository;
    private final OrganizationMapper mapper;
    private final EntityManager entityManager;
    private final TenantAccessGuard tenantAccess;

    public OrganizationService(
            OrganizationRepository repository,
            OrganizationMapper mapper,
            EntityManager entityManager,
            TenantAccessGuard tenantAccess) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.tenantAccess = tenantAccess;
    }

    @Transactional
    public OrganizationResponse create(OrganizationCreateRequest request) {
        validateTaxIdentifierAvailable(request.taxIdentifier(), null);
        Organization organization = repository.save(mapper.toEntity(request));
        entityManager.flush();
        entityManager.refresh(organization);
        return mapper.toResponse(organization);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> list(Pageable pageable) {
        validateSort(pageable);
        AuthenticatedOrganizationContext context = tenantAccess.current();
        if (context.isPlatformAdministrator()) {
            return PageResponse.from(repository.findAllByDeletedAtIsNull(pageable), mapper::toResponse);
        }
        return PageResponse.from(
                repository.findAllByIdAndStatusAndDeletedAtIsNull(
                        context.organizationId(), OrganizationStatus.ACTIVE, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(Long organizationId) {
        tenantAccess.requireTenant(organizationId);
        return mapper.toResponse(requireAccessibleOrganization(organizationId));
    }

    @Transactional
    public OrganizationResponse update(Long organizationId, OrganizationUpdateRequest request) {
        tenantAccess.requireTenant(organizationId);
        Organization organization = requireAccessibleOrganization(organizationId);
        validateTaxIdentifierAvailable(request.taxIdentifier(), organizationId);
        mapper.update(organization, request);
        entityManager.flush();
        entityManager.refresh(organization);
        return mapper.toResponse(organization);
    }

    private Organization requireOrganization(Long organizationId) {
        return repository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private Organization requireAccessibleOrganization(Long organizationId) {
        if (tenantAccess.current().isPlatformAdministrator()) {
            return requireOrganization(organizationId);
        }
        return repository.findByIdAndStatusAndDeletedAtIsNull(organizationId, OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private void validateTaxIdentifierAvailable(String taxIdentifier, Long currentId) {
        String normalized = normalize(taxIdentifier);
        if (normalized == null) {
            return;
        }
        boolean duplicate = currentId == null
                ? repository.existsByTaxIdentifierAndDeletedAtIsNull(normalized)
                : repository.existsByTaxIdentifierAndDeletedAtIsNullAndIdNot(normalized, currentId);
        if (duplicate) {
            throw new ConflictException(
                    "ORGANIZATION_TAX_IDENTIFIER_EXISTS",
                    "An organization with tax identifier %s already exists".formatted(normalized));
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_PROPERTIES.contains(order.getProperty()));
        if (invalid) {
            throw new InvalidRequestException("Unsupported organization sort property");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
