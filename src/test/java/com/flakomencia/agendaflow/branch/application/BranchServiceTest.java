package com.flakomencia.agendaflow.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.branch.api.BranchCreateRequest;
import com.flakomencia.agendaflow.branch.api.BranchResponse;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

class BranchServiceTest {

    private BranchRepository branchRepository;
    private OrganizationRepository organizationRepository;
    private BranchMapper mapper;
    private EntityManager entityManager;
    private TenantAccessGuard tenantAccess;
    private BranchService service;

    @BeforeEach
    void setUp() {
        branchRepository = mock(BranchRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        mapper = mock(BranchMapper.class);
        entityManager = mock(EntityManager.class);
        tenantAccess = mock(TenantAccessGuard.class);
        when(tenantAccess.current()).thenReturn(
                new AuthenticatedOrganizationContext(1L, 1L, 1L, Set.of("PLATFORM_ADMIN"), Set.of()));
        service = new BranchService(branchRepository, organizationRepository, mapper, entityManager, tenantAccess);
    }

    @Test
    void createsBranchInsideOrganization() {
        Organization organization = new Organization("AgendaFlow");
        BranchCreateRequest request = createRequest("Central", "CENTRAL");
        Branch branch = new Branch(organization, "Central");
        BranchResponse response = mock(BranchResponse.class);
        when(organizationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
        when(mapper.toEntity(organization, request)).thenReturn(branch);
        when(branchRepository.save(branch)).thenReturn(branch);
        when(mapper.toResponse(branch)).thenReturn(response);

        assertThat(service.create(1L, request)).isSameAs(response);

        verify(branchRepository).existsByOrganization_IdAndName(1L, "Central");
        verify(branchRepository).existsByOrganization_IdAndCode(1L, "CENTRAL");
        verify(entityManager).refresh(branch);
    }

    @Test
    void returnsNotFoundForMissingBranch() {
        Organization organization = new Organization("AgendaFlow");
        when(organizationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
        when(branchRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(50L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 50L))
                .isInstanceOf(BranchNotFoundException.class)
                .hasMessage("Branch 50 was not found in organization 1");
    }

    @Test
    void preventsCrossOrganizationAccess() {
        Organization organization = new Organization("Other organization");
        when(organizationRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(organization));
        when(branchRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(50L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, 50L))
                .isInstanceOf(BranchNotFoundException.class);

        verify(branchRepository).findByIdAndOrganization_IdAndDeletedAtIsNull(50L, 2L);
    }

    @Test
    void rejectsDuplicateBranchName() {
        Organization organization = new Organization("AgendaFlow");
        BranchCreateRequest request = createRequest("Central", "OTHER");
        when(organizationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
        when(branchRepository.existsByOrganization_IdAndName(1L, "Central")).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Central");

        verify(mapper, never()).toEntity(organization, request);
    }

    private BranchCreateRequest createRequest(String name, String code) {
        return new BranchCreateRequest(
                name, code, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
