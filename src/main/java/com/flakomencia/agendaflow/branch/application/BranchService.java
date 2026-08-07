package com.flakomencia.agendaflow.branch.application;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.branch.api.BranchCreateRequest;
import com.flakomencia.agendaflow.branch.api.BranchResponse;
import com.flakomencia.agendaflow.branch.api.BranchUpdateRequest;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

@Service
public class BranchService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "name", "code", "active", "createdAt", "updatedAt");

    private final BranchRepository branchRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchMapper mapper;
    private final EntityManager entityManager;
    private final TenantAccessGuard tenantAccess;

    public BranchService(
            BranchRepository branchRepository,
            OrganizationRepository organizationRepository,
            BranchMapper mapper,
            EntityManager entityManager,
            TenantAccessGuard tenantAccess) {
        this.branchRepository = branchRepository;
        this.organizationRepository = organizationRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.tenantAccess = tenantAccess;
    }

    @Transactional
    public BranchResponse create(Long organizationId, BranchCreateRequest request) {
        tenantAccess.requireTenant(organizationId);
        Organization organization = requireOrganization(organizationId);
        validateDuplicates(organizationId, request.name(), request.code(), null);
        Branch branch = branchRepository.save(mapper.toEntity(organization, request));
        entityManager.flush();
        entityManager.refresh(branch);
        return mapper.toResponse(branch);
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> list(Long organizationId, Pageable pageable) {
        tenantAccess.requireTenant(organizationId);
        requireOrganization(organizationId);
        validateSort(pageable);
        return PageResponse.from(
                branchRepository.findAllByOrganization_IdAndDeletedAtIsNull(organizationId, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BranchResponse get(Long organizationId, Long branchId) {
        tenantAccess.requireTenant(organizationId);
        requireOrganization(organizationId);
        return mapper.toResponse(requireBranch(organizationId, branchId));
    }

    @Transactional
    public BranchResponse update(Long organizationId, Long branchId, BranchUpdateRequest request) {
        tenantAccess.requireTenant(organizationId);
        requireOrganization(organizationId);
        Branch branch = requireBranch(organizationId, branchId);
        validateDuplicates(organizationId, request.name(), request.code(), branchId);
        mapper.update(branch, request);
        entityManager.flush();
        entityManager.refresh(branch);
        return mapper.toResponse(branch);
    }

    private Organization requireOrganization(Long organizationId) {
        if (!tenantAccess.current().isPlatformAdministrator()) {
            return organizationRepository
                    .findByIdAndStatusAndDeletedAtIsNull(organizationId, OrganizationStatus.ACTIVE)
                    .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        }
        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private Branch requireBranch(Long organizationId, Long branchId) {
        return branchRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(branchId, organizationId)
                .orElseThrow(() -> new BranchNotFoundException(organizationId, branchId));
    }

    private void validateDuplicates(Long organizationId, String name, String code, Long currentId) {
        String normalizedName = name.trim();
        String normalizedCode = normalize(code);
        boolean duplicateName = currentId == null
                ? branchRepository.existsByOrganization_IdAndName(organizationId, normalizedName)
                : branchRepository.existsByOrganization_IdAndNameAndIdNot(organizationId, normalizedName, currentId);
        if (duplicateName) {
            throw new ConflictException(
                    "BRANCH_NAME_EXISTS",
                    "A branch named %s already exists in organization %d".formatted(normalizedName, organizationId));
        }
        if (normalizedCode == null) {
            return;
        }
        boolean duplicateCode = currentId == null
                ? branchRepository.existsByOrganization_IdAndCode(organizationId, normalizedCode)
                : branchRepository.existsByOrganization_IdAndCodeAndIdNot(organizationId, normalizedCode, currentId);
        if (duplicateCode) {
            throw new ConflictException(
                    "BRANCH_CODE_EXISTS",
                    "A branch with code %s already exists in organization %d".formatted(normalizedCode, organizationId));
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_PROPERTIES.contains(order.getProperty()));
        if (invalid) {
            throw new InvalidRequestException("Unsupported branch sort property");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
